package raidone.robot.submodules;

import com.ctre.phoenix.motion.SetValueMotionProfile;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.FollowerType;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.RemoteSensorSource;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.can.TalonSRXConfiguration;
import com.ctre.phoenix.sensors.PigeonIMU;
import com.ctre.phoenix.sensors.PigeonIMU_StatusFrame;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpiutil.math.MathUtil;
import raidone.robot.Constants;
import raidone.robot.Constants.DriveConstants;
import raidone.robot.dashboard.Tab;
import raidone.robot.wrappers.*;
import raidone.robot.pathing.Path;
import raidone.robot.pathing.ProfileFollower;
import raidone.robot.utils.EncoderUtils;

public class Drive extends Submodule {

    public static enum GearShift {
        HIGH, LOW
    }

    public static enum ControlState {
        OPEN_LOOP, PATH_FOLLOWING
    }

    private static Drive instance = null;

    public static Drive getInstance() {
        if (instance == null) {
            instance = new Drive();
        }
        return instance;
    }

    private Drive() {
    }

    private LazyTalonSRX leftLeader;
    private LazyVictorSPX leftFollowerA;
    private LazyVictorSPX leftFollowerB;
    private LazyTalonSRX rightLeader; 
    private LazyVictorSPX rightFollowerA;
    private LazyVictorSPX rightFollowerB;

    // Should be references to one of the 4 motors
    private LazyTalonSRX profilingLeader;
    private LazyTalonSRX profilingFollower;

    private InactiveDoubleSolenoid gearShiftSolenoid;

    private PigeonIMU pigeon;

    // Control states
    private ControlState controlState;

    // Profile follower
    private ProfileFollower mpFollower;

    private GearShift currentGearShift;

    private double quickStopAccumulator = 0.0;

    // Output
    private double outputLeftDrive = 0.0;
    private double outputRightDrive = 0.0;
    private int outputClosedLoop = 0;

    private NetworkTableEntry gearShiftEntry = Shuffleboard.getTab(Tab.MAIN)
        .add("Gear Shift", "EMPTY")
        .withWidget(BuiltInWidgets.kTextView)
        .withSize(1, 1)
        .withPosition(3, 2)
        .getEntry();
    private NetworkTableEntry leftEncoderEntry = Shuffleboard.getTab(Tab.DEBUG)
        .add("Left Distance (in)", 0.0)
        .withWidget(BuiltInWidgets.kTextView)
        .withSize(1, 1)
        .withPosition(0, 0)
        .getEntry();
    private NetworkTableEntry rightEncoderEntry = Shuffleboard.getTab(Tab.DEBUG)
        .add("Right Distance (in)", 0.0)
        .withWidget(BuiltInWidgets.kTextView)
        .withSize(1, 1)
        .withPosition(1, 0)
        .getEntry();

    @Override
    public void onInit() {
        // Motors
        leftLeader = new LazyTalonSRX(DriveConstants.LEFT_LEADER_ID);
        configureMotor(leftLeader, DriveConstants.LEFT_INVERSION);
        leftLeader.setSensorPhase(true);

        leftFollowerA = new LazyVictorSPX(DriveConstants.LEFT_FOLLOWERA_ID);
        configureMotor(leftFollowerA, DriveConstants.LEFT_INVERSION);
        leftFollowerA.follow(leftLeader);

        leftFollowerB = new LazyVictorSPX(DriveConstants.LEFT_FOLLOWERB_ID);
        configureMotor(leftFollowerB, DriveConstants.LEFT_INVERSION);
        leftFollowerB.follow(leftLeader);

        rightLeader = new LazyTalonSRX(DriveConstants.RIGHT_LEADER_ID);
        configureMotor(rightLeader, DriveConstants.RIGHT_INVERSION);
        rightLeader.setSensorPhase(true);

        rightFollowerA = new LazyVictorSPX(DriveConstants.RIGHT_FOLLOWERA_ID);
        configureMotor(rightFollowerA, DriveConstants.RIGHT_INVERSION);
        rightFollowerA.follow(rightLeader);

        rightFollowerB = new LazyVictorSPX(DriveConstants.RIGHT_FOLLOWERB_ID);
        configureMotor(rightFollowerB, DriveConstants.RIGHT_INVERSION);
        rightFollowerB.follow(rightLeader);
        leftLeader.configOpenloopRamp(0.1);
        rightLeader.configOpenloopRamp(0.1);

        // Pigeon IMU
        pigeon = new PigeonIMU(DriveConstants.PIGEON_ID);
        pigeon.configFactoryDefault();

        /*
         * Setup the profiling leader & follower
         * 
         * Note: if this changes, the argument to configureMotorClosedLoop must
         * reflect the inversion of the leader motor
         */
        profilingLeader = rightLeader;
        profilingFollower = leftLeader;

        // Must be called after the pigeon is initialized
        configureMotorClosedLoop(DriveConstants.RIGHT_INVERSION);

        // Gear shift
        gearShiftSolenoid = new InactiveDoubleSolenoid(DriveConstants.GEARSHIFT_FORWARD_ID,
                DriveConstants.GEARSHIFT_REVERSE_ID);

        // Control state
        controlState = ControlState.OPEN_LOOP;
    }

    /**
     * Configures a motor controller.
     * 
     * @param motor     the motor controller to configure
     * @param inversion whether to invert the motor output
     */
    private void configureMotor(LazyTalonSRX motor, InvertType inversion) {
        motor.configFactoryDefault();
        motor.setNeutralMode(NeutralMode.Coast);
        motor.setInverted(inversion);
    }

    /**
     * Configures a motor controller.
     * 
     * @param motor     the motor controller to configure
     * @param inversion whether to invert the motor output
     */
    private void configureMotor(LazyVictorSPX motor, InvertType inversion) {
        motor.configFactoryDefault();
        motor.setNeutralMode(NeutralMode.Coast);
        motor.setInverted(inversion);
    }

    /**
     * Configures the motor controllers for closed-loop control.
     * 
     * @param leaderInversion whether the profiling leader is inverted
     */
    private void configureMotorClosedLoop(InvertType leaderInversion) {
        TalonSRXConfiguration leaderConfig = new TalonSRXConfiguration();
        TalonSRXConfiguration followerConfig = new TalonSRXConfiguration();

        // Use the integrated encoder on the falcons as feedback
        // leaderConfig.primaryPID.selectedFeedbackSensor = FeedbackDevice.QuadEncoder;
        followerConfig.primaryPID.selectedFeedbackSensor = FeedbackDevice.QuadEncoder;

        // Configure the follower encoder as a remote sensor for the leader
        leaderConfig.remoteFilter0.remoteSensorDeviceID = profilingFollower.getDeviceID();
        leaderConfig.remoteFilter0.remoteSensorSource = RemoteSensorSource.TalonSRX_SelectedSensor;

        // Configure robot distance math
        setRobotDistanceConfigs(leaderInversion, leaderConfig);

        // Configure the Pigeon as the other Remote Slot on the leader
        leaderConfig.remoteFilter1.remoteSensorDeviceID = pigeon.getDeviceID();
        leaderConfig.remoteFilter1.remoteSensorSource = RemoteSensorSource.Pigeon_Yaw;
        leaderConfig.auxiliaryPID.selectedFeedbackSensor = FeedbackDevice.RemoteSensor1;
        leaderConfig.auxiliaryPID.selectedFeedbackCoefficient = DriveConstants.PIGEON_SCALE;
        leaderConfig.auxPIDPolarity = DriveConstants.AUX_POLARITY;

        // PIDF Gains for the distance part
        leaderConfig.slot0.kP = DriveConstants.PRIMARY_P;
        leaderConfig.slot0.kI = DriveConstants.PRIMARY_I;
        leaderConfig.slot0.kD = DriveConstants.PRIMARY_D;
        leaderConfig.slot0.kF = DriveConstants.PRIMARY_F;
        leaderConfig.slot0.integralZone = DriveConstants.PRIMARY_INT_ZONE;

        // PIDF Gains for turning part
        leaderConfig.slot1.kP = DriveConstants.AUX_P;
        leaderConfig.slot1.kI = DriveConstants.AUX_I;
        leaderConfig.slot1.kD = DriveConstants.AUX_D;
        leaderConfig.slot1.kF = DriveConstants.AUX_F;
        leaderConfig.slot1.integralZone = DriveConstants.AUX_INT_ZONE;

        leaderConfig.neutralDeadband = DriveConstants.DRIVE_NEUTRAL_DEADBAND;
        followerConfig.neutralDeadband = DriveConstants.DRIVE_NEUTRAL_DEADBAND;

        // leaderConfig.openloopRamp = 1.0;
        // followerConfig.openloopRamp = 1.0;

        // Apply all settings
        profilingLeader.configAllSettings(leaderConfig);
        profilingFollower.configAllSettings(followerConfig);

        // Set status frame periods to ensure we don't have stale data
        profilingLeader.setStatusFramePeriod(StatusFrame.Status_12_Feedback1, 20,
                Constants.TIMEOUT_MS);
        profilingLeader.setStatusFramePeriod(StatusFrame.Status_13_Base_PIDF0, 20,
                Constants.TIMEOUT_MS);
        profilingLeader.setStatusFramePeriod(StatusFrame.Status_14_Turn_PIDF1, 20,
                Constants.TIMEOUT_MS);
        profilingLeader.setStatusFramePeriod(StatusFrame.Status_10_Targets, 20,
                Constants.TIMEOUT_MS);
        profilingFollower.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 5,
                Constants.TIMEOUT_MS);
        pigeon.setStatusFramePeriod(PigeonIMU_StatusFrame.CondStatus_9_SixDeg_YPR, 5,
                Constants.TIMEOUT_MS);

        profilingLeader.changeMotionControlFramePeriod(DriveConstants.TRANSMIT_PERIOD_MS);
        profilingLeader.configMotionProfileTrajectoryPeriod(DriveConstants.BASE_TRAJ_PERIOD_MS,
                Constants.TIMEOUT_MS);

        mpFollower = new ProfileFollower(profilingLeader);
    }

    /**
     * Determines if SensorSum or SensorDiff should be used for combining
     * left/right sensors into Robot Distance.
     * 
     * Assumes Aux Position is set as Remote Sensor 0.
     * 
     * configAllSettings must still be called on the master config after this
     * function modifies the config values.
     * 
     * Source: https://shorturl.at/oqvCN (CTRE GitHub)
     * 
     * @param masterInvertType Invert of the Master Talon
     * @param masterConfig     Configuration object to fill
     */
    private void setRobotDistanceConfigs(InvertType masterInvertType,
            TalonSRXConfiguration masterConfig) {
        masterConfig.sum0Term = FeedbackDevice.RemoteSensor0; // Aux Selected Sensor
        masterConfig.sum1Term = FeedbackDevice.QuadEncoder; // Local IntegratedSensor
        masterConfig.primaryPID.selectedFeedbackSensor = FeedbackDevice.SensorSum;
        masterConfig.primaryPID.selectedFeedbackCoefficient = 0.5;
    }

    /**
     * Resets all outputs on start.
     * 
     * @param timestamp
     */
    @Override
    public void onStart(double timestamp) {
        controlState = ControlState.OPEN_LOOP;

        outputLeftDrive = 0.0;
        outputRightDrive = 0.0;
        outputClosedLoop = 0;

        setGearShift(GearShift.LOW);

        profilingLeader.clearMotionProfileTrajectories();
    }

    /**
     * Updates the profile follower if currently in path following mode.
     * 
     * @param timestamp
     */
    @Override
    public void update(double timestamp) {
        if (controlState == ControlState.PATH_FOLLOWING) {
            mpFollower.update();
            outputClosedLoop = mpFollower.getOutput();
        }
        gearShiftEntry.setString(currentGearShift.toString());
        leftEncoderEntry.setNumber(
            EncoderUtils.ticksToInches(
                leftLeader.getSelectedSensorPosition(), currentGearShift)
        );
        rightEncoderEntry.setNumber(
            EncoderUtils.ticksToInches(
                -rightLeader.getSelectedSensorPosition(), currentGearShift)
        );
    }

    /**
     * Runs the motors with different control modes depending on the state.
     */
    @Override
    public void run() {
        switch (controlState) {
            case OPEN_LOOP:
                leftLeader.set(ControlMode.PercentOutput, outputLeftDrive);
                rightLeader.set(ControlMode.PercentOutput, outputRightDrive);
                break;
            case PATH_FOLLOWING:
                profilingLeader.set(ControlMode.MotionProfileArc, outputClosedLoop);
                profilingFollower.follow(profilingLeader, FollowerType.AuxOutput1);
                break;
        }
    }

    /**
     * Stops all the motors. Also changes the control state to open-loop.
     */
    @Override
    public void stop() {
        controlState = ControlState.OPEN_LOOP;

        outputLeftDrive = 0.0;
        outputRightDrive = 0.0;
        outputClosedLoop = SetValueMotionProfile.Disable.value;

        leftLeader.set(ControlMode.Disabled, 0.0);
        rightLeader.set(ControlMode.Disabled, 0.0);
    }

    /**
     * Zeros all encoders & the pigeon to 0.
     */
    @Override
    public void zero() {
        /**
         * setSelectedSensorPosition would set the sensor sum for the PID, which is
         * not what we want to do.
         */
        leftLeader.getSensorCollection().setQuadraturePosition(0, Constants.TIMEOUT_MS);
        rightLeader.getSensorCollection().setQuadraturePosition(0, Constants.TIMEOUT_MS);
        // leftLeader.setSelectedSensorPosition(0.0, 0, Constants.TIMEOUT_MS);
        // rightLeader.setSelectedSensorPosition(0.0, 0, Constants.TIMEOUT_MS);
        pigeon.setYaw(0.0);
    }

    /**
     * Switches the base to open-loop control mode.
     * 
     * Note: Should be called after finishing motion profiling.
     */
    public void setOpenLoop() {
        controlState = ControlState.OPEN_LOOP;
    }

    /**
     * Tank drive mode for open-loop control.
     * 
     * @param left    left percent output in [-1, 1]
     * @param right   right percent output in [-1, 1]
     * @param reverse whether to reverse the inputs or not
     */
    public void tank(double left, double right, boolean reverse) {
        if (left == 0 & Math.abs(right) > 0)
            left = -Math.copySign(0.0001, right);
        else if (right == 0 & Math.abs(left) > 0)
            right = -Math.copySign(0.0001, left);

        // if(left * right >= 0) {
        //     double l = Math.abs(left);
        //     double r = Math.abs(right);
        //     if(l < r * 0.10)
        //         left = right * 0.10;
        //     else if(r < l * 0.10)
        //         right = left * 0.10;
        // }

        if (reverse) {
            outputLeftDrive = -left;
            outputRightDrive = -right;
            return;
        }

        outputLeftDrive = left;
        outputRightDrive = right;
    }

    /**
     * Arcade drive mode for open-loop control.
     * 
     * @param leftJoystick  value of the left joystick in [-1, 1]
     * @param rightJoystick value of the right joystick in [-1, 1]
     * @param reverse       whether to reverse the inputs or not
     */
    public void arcade(double leftJoystick, double rightJoystick, boolean reverse) {
        if (reverse) {
            leftJoystick *= -1;
            rightJoystick *= -1;
        }
        outputLeftDrive = leftJoystick + rightJoystick;
        outputRightDrive = leftJoystick - rightJoystick;
    }

    /**
     * Curvature drive mode for open-loop control.
     * 
     * @param xSpeed      The robot's speed along the X axis [-1.0, 1.0].
     * @param zRotation   The robot's rotation rate around the Z axis [-1.0,
     *                    1.0]. Clockwise is positive.
     * @param isQuickTurn If set, overrides constant-curvature turning for
     *                    turn-in-place maneuvers.
     */
    public void curvatureDrive(double xSpeed, double zRotation, boolean isQuickTurn) {
        xSpeed = MathUtil.clamp(xSpeed, -1.0, 1.0);
        zRotation = MathUtil.clamp(zRotation, -1.0, 1.0);

        double angularPower;
        boolean overPower;

        if (isQuickTurn) {
            if (Math.abs(xSpeed) < DriveConstants.QUICK_STOP_THRESHOLD) {
                quickStopAccumulator = (1 - DriveConstants.QUICK_STOP_ALPHA) * quickStopAccumulator
                        + DriveConstants.QUICK_STOP_ALPHA * MathUtil.clamp(zRotation, -1.0, 1.0)
                                * 2;
            }
            overPower = true;
            angularPower = zRotation;
        } else {
            overPower = false;
            angularPower = Math.abs(xSpeed) * zRotation - quickStopAccumulator;

            if (quickStopAccumulator > 1) {
                quickStopAccumulator -= 1;
            } else if (quickStopAccumulator < -1) {
                quickStopAccumulator += 1;
            } else {
                quickStopAccumulator = 0.0;
            }
        }

        double leftMotorOutput = xSpeed + angularPower;
        double rightMotorOutput = xSpeed - angularPower;

        // Reduce both outputs to acceptable range if rotation is overpowered
        if (overPower) {
            if (leftMotorOutput > 1.0) {
                rightMotorOutput -= leftMotorOutput - 1.0;
                leftMotorOutput = 1.0;
            } else if (rightMotorOutput > 1.0) {
                leftMotorOutput -= rightMotorOutput - 1.0;
                rightMotorOutput = 1.0;
            } else if (leftMotorOutput < -1.0) {
                rightMotorOutput -= leftMotorOutput + 1.0;
                leftMotorOutput = -1.0;
            } else if (rightMotorOutput < -1.0) {
                leftMotorOutput -= rightMotorOutput + 1.0;
                rightMotorOutput = -1.0;
            }
        }

        // Normalize the wheel speeds
        double maxMagnitude = Math.max(Math.abs(leftMotorOutput), Math.abs(rightMotorOutput));
        if (maxMagnitude > 1.0) {
            leftMotorOutput /= maxMagnitude;
            rightMotorOutput /= maxMagnitude;
        }
        outputLeftDrive = leftMotorOutput;
        outputRightDrive = rightMotorOutput;
    }

    /**
     * Sets the brake mode to brake or coast.
     * 
     * @param brake whether to brake or not
     */
    public void setBrakeMode(boolean brake) {
        if (brake) {
            rightLeader.setNeutralMode(NeutralMode.Brake);
            rightFollowerA.setNeutralMode(NeutralMode.Brake);
            rightFollowerB.setNeutralMode(NeutralMode.Brake);
            leftLeader.setNeutralMode(NeutralMode.Brake);
            leftFollowerA.setNeutralMode(NeutralMode.Brake);
            leftFollowerB.setNeutralMode(NeutralMode.Brake);
        } else {
            rightLeader.setNeutralMode(NeutralMode.Coast);
            rightFollowerA.setNeutralMode(NeutralMode.Coast);
            rightFollowerB.setNeutralMode(NeutralMode.Coast);
            leftLeader.setNeutralMode(NeutralMode.Coast);
            leftFollowerA.setNeutralMode(NeutralMode.Coast);
            leftFollowerB.setNeutralMode(NeutralMode.Coast);
        }
    }

    /**
     * Shifts the gears depending on the mode.
     * 
     * @param mode the gear shift
     */
    public void setGearShift(GearShift mode) {
        currentGearShift = mode;
        gearShiftSolenoid.set(gearSolenoidValue(mode));
    }

    /**
     * Converts from a GearShift to a DoubleSolenoid value.
     * 
     * @param gear the gear shift
     * @return a DoubleSolenoid value
     */
    private Value gearSolenoidValue(GearShift gear) {
        if (gear == GearShift.HIGH) {
            return Value.kForward;
        }
        if (gear == GearShift.LOW) {
            return Value.kReverse;
        }
        return Value.kOff;
    }

    /**
     * Makes the drive start following a Path.
     * 
     * @param path the path to follow
     */
    public void setDrivePath(Path path) {
        if (mpFollower != null) {
            // Stops & resets everything
            stop();
            zero();
            // The path may start at a different angle at times
            pigeon.setYaw(path.getFirstPoint().angle.orElse(0.0));
            mpFollower.reset();

            mpFollower.setGearShift(currentGearShift);
            mpFollower.setReverse(path.isReversed());
            mpFollower.start(path.getPathPoints());
            controlState = ControlState.PATH_FOLLOWING;
        }
    }

    /**
     * Returns whether the drive has finished following a path.
     * 
     * @return if the drive is finished pathing
     */
    public boolean isFinishedWithPath() {
        if (mpFollower == null || controlState != ControlState.PATH_FOLLOWING) {
            return false;
        }
        return mpFollower.isFinished();
    }

    public double getPigeonValue(){
        double[] ypr_deg = new double[3];
        pigeon.getYawPitchRoll(ypr_deg);
        return ypr_deg[0];
    }
}
