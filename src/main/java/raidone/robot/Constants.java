package raidone.robot;

import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.NeutralMode;

import raidone.robot.utils.InterpolatingDouble;
import raidone.robot.utils.InterpolatingTreeMap;

public class Constants {
    /**
     * Talon SRX IDs
     * RightM 01
     * LeftM 11
     * 
     * Victor SPX IDs
     * RightVictorA 02
     * RightVictorB 03
     * LeftVictorA 12
     * LeftVictorB 13
     * 
     * Talon FX
     * RightThroat 21
     * LeftThroat 22
     * Intake 31
     * RightShooter 41
     * LeftShooter 42
     */

    /**
     * Drivetrain Constants
     */
    public static final class DriveConstants {
        public static final int LEFT_LEADER_ID = 11;
        public static final int LEFT_FOLLOWERA_ID = 12;
        public static final int LEFT_FOLLOWERB_ID =13;
        public static final int RIGHT_LEADER_ID = 01;
        public static final int RIGHT_FOLLOWERA_ID = 02;
        public static final int RIGHT_FOLLOWERB_ID = 03;

        public static final int GEARSHIFT_FORWARD_ID = 1; 
        public static final int GEARSHIFT_REVERSE_ID = 0;

        public static final int PIGEON_ID = 0;

        public static final InvertType LEFT_INVERSION = InvertType.None;
        public static final InvertType RIGHT_INVERSION = InvertType.InvertMotorOutput;

        public static final double HIGH_GEAR_RATIO = 1;
        public static final double LOW_GEAR_RATIO = 1;
        // 12-20 20-36 36-30
        
        public static final double WHEEL_DIAMETER_INCHES = 6.0;

        // Closed-loop constants
        public static final double DRIVE_NEUTRAL_DEADBAND = 0.01;
        public static final int PID_PRIMARY_SLOT = 0;
        public static final int PID_AUX_SLOT = 1;
        public static final double PIGEON_SCALE = 3600.0 / 8192.0;

        public static final double PRIMARY_F = 1 * 1023.0 / 5000;
        public static final double PRIMARY_P = 0.03; // 1023 / (30 * 2000)
        public static final double PRIMARY_I = 0;
        public static final double PRIMARY_D = 0;
        public static final int PRIMARY_INT_ZONE = 100;

        public static final double AUX_F = 0;
        public static final double AUX_P = 2.2;
        public static final double AUX_I = 0;
        public static final double AUX_D = 0.6;//4.0;
        public static final int AUX_INT_ZONE = 20;
        public static final boolean AUX_POLARITY = false;

        public static final int BASE_TRAJ_PERIOD_MS = 0;
        public static final int SENSOR_UNITS_PER_ROTATION = 8192;
        public static final double SENSOR_UNITS_PER_INCH_LOW_GEAR = 
            SENSOR_UNITS_PER_ROTATION * LOW_GEAR_RATIO / (WHEEL_DIAMETER_INCHES * Math.PI);
        public static final double SENSOR_UNITS_PER_INCH_HIGH_GEAR = 
            SENSOR_UNITS_PER_ROTATION * HIGH_GEAR_RATIO / (WHEEL_DIAMETER_INCHES * Math.PI);
        public static final int MIN_POINTS_IN_TALON = 10;
        public static final int TRANSMIT_PERIOD_MS = 3;

        public static final double DEFAULT_CRUISE_VELOCITY = 2; // 4.5, 1m
        public static final double DEFAULT_TARGET_ACCELERATION = 1.5; //5, 1

        // Joystick to Output mapping
        public static final double JOYSTICK_EXPONENT = 1.2;
        public static final double JOYSTICK_COEFFICIENT = 1;

        public static final double QUICK_STOP_THRESHOLD = 0.2;
        public static final double QUICK_STOP_ALPHA = 0.1;
    }

    /**
     * Shooter Constants
     */
    public static final class ShooterConstants {
        public static final int LEFT_MOTOR_ID = 42;
        public static final int RIGHT_MOTOR_ID = 41;

        public static final NeutralMode NEUTRAL_MODE = NeutralMode.Coast;
        public static final InvertType LEFT_INVERSION = InvertType.InvertMotorOutput;
        public static final InvertType RIGHT_INVERSION = InvertType.None;

        public static final double MAX_SPEED = 20000; // in ticks per 100ms
        public static final double FAKE_MAX_SPEED = 17000; // in ticks per 100ms
        public static final double ERROR_TOLERANCE = 250;
        public static final double UP_TO_SPEED_DURATION = 0.5; // in seconds

        public static final double K_F = 1023.0 / MAX_SPEED;
        public static final double K_P = 0.6;
        public static final double K_I = 0; // Shouldn't be touched
        public static final double K_D = 5.0; // Shouldn't be touched
        public static final int K_INTEGRAL_ZONE = 0; // Shouldn't be touched
    }

    /**
     * Intake Constants
     */
    public static final class IntakeConstants {
        public static final int MOTOR_ID = 31;

        public static final NeutralMode NEUTRAL_MODE = NeutralMode.Brake;
        public static final InvertType INVERSION = InvertType.None;

        public static final int INTAKE_FORWARD_ID = 2;
        public static final int INTAKE_REVERSE_ID = 3;

        public static final double CONTROL_SCALING_FACTOR = 1.0;
    }

    /**
     * Throat Constants
     */
    public static final class ThroatConstants {
        public static final int LEFT_MOTOR_ID = 22;
        public static final int RIGHT_MOTOR_ID = 21;

        public static final NeutralMode NEUTRAL_MODE = NeutralMode.Brake;
        public static final InvertType LEFT_INVERSION = InvertType.InvertMotorOutput;
        public static final InvertType RIGHT_INVERSION = InvertType.None;

        public static final int THROAT_FORWARD_ID = 4;
        public static final int THROAT_REVERSE_ID = 5;

        public static final double CONTROL_SCALING_FACTOR = 1.0;
    }

    /**
     * Limelight Constants
     */
    public static final class LimelightConstants {
        public static final String NAME = "limelight";

        public static final double MOUNTING_ANGLE = 31.4; // in degrees
        public static final double MOUNTING_HEIGHT = 0.56; // in meters

        // TODO: Improve the constants
        public static final double AIM_KP = 0.035;
        public static final double AIM_KI = 0.004;
        public static final double AIM_KD = 0.001;
        public static final double ANGLE_ADJUST_THRESHOLD = 2.0;

        public static final double AIM_ON_TARGET_DURATION = 0.2;
    }

    /**
     * Field Constants
     */
    public static final class FieldConstants {
        public static final double GOAL_HEIGHT = 2.3; // in meters
    }

    // Distance (m/s) to Speed (percent) Lookup Table
    public static final InterpolatingTreeMap<InterpolatingDouble, InterpolatingDouble> 
        DISTANCE_TO_HOOD_TICKS = new InterpolatingTreeMap<>(20);
    static {
        /*DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(2.94), new InterpolatingDouble(4683.0));
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(4.55), new InterpolatingDouble(6000.0));
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(1.38), new InterpolatingDouble(2000.0));
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(5.07), new InterpolatingDouble(1.0));
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(4.0), new InterpolatingDouble(1.0));*/

        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(2.96), new InterpolatingDouble(5773.0));
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(1.58), new InterpolatingDouble(2700.0)); // weird matthew
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(2.05), new InterpolatingDouble(4025.0));

        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(5.70), new InterpolatingDouble(6871.0));
        DISTANCE_TO_HOOD_TICKS.put(new InterpolatingDouble(1.0), new InterpolatingDouble(1.0));
    }
    
    /**
     * Joystick Constants
     */
    public static final double JOYSTICK_DEADBAND = 0.05;

    public static final int TIMEOUT_MS = 10;
}