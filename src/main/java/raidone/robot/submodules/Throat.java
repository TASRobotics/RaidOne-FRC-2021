package raidone.robot.submodules;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import jdk.jfr.Percentage;

//Talon FX
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.revrobotics.CANEncoder;
//Sparkmax
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.CANPIDController;

import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.FollowerType;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFXConfiguration;
import com.playingwithfusion.TimeOfFlight;

public class Throat {
    //THROAT MOTORS
    public static final WPI_TalonFX _throatright = new WPI_TalonFX(21);
    public static final WPI_TalonFX _throatleft = new WPI_TalonFX(22);
    public static final TalonFXConfiguration config = new TalonFXConfiguration();

    //THROAT SENSOR(S)
    public static final TimeOfFlight _throatSensor = new TimeOfFlight(0);

    public enum Speed { 
        DOWN, 
        UP, 
        UP_SLOW, 
        DOWN_SLOW, 
        STOP
    }

     //Constructor 
    public Throat(){
        _throatright.configAllSettings(config);
        _throatleft.setInverted(InvertType.InvertMotorOutput);
        _throatleft.follow(_throatright, FollowerType.PercentOutput);
    }
    
    //CONSTANTS
    public static final double BALL_DETECTED_VALUE = 80.0;

    //HELPER FUNCTIONS
    public static void set(double percent){
        _throatright.set(percent);
    }

    public static void setState(Speed set){
        if(set == Speed.DOWN){
            set(-1);
        }
        if(set == Speed.UP){
            set(1);
        }
        if(set == Speed.UP_SLOW){
            set(0.3);
        }
        if(set == Speed.DOWN_SLOW){
            set(-0.3);
        } 
        if(set == Speed.STOP){
            set(0);
        }
    }

    public static void index(){
        if(_throatSensor.getRange() < BALL_DETECTED_VALUE){ //idk, tune or smth
            set(0);
        } else {
            set(0.16);
        }
    }
}
