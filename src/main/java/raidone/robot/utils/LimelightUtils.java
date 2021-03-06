package raidone.robot.utils;

import raidone.robot.Constants.FieldConstants;
import raidone.robot.Constants.LimelightConstants;

public class LimelightUtils {

    /**
     * Returns the estimated distance to the goal in meters.
     * 
     * @param angle ty from the limelight in degrees
     * @return estimated distance in meters
     */
    public static double estimateDistance(double angle) {
        return (FieldConstants.GOAL_HEIGHT - LimelightConstants.MOUNTING_HEIGHT) /
            Math.tan(Math.toRadians(LimelightConstants.MOUNTING_ANGLE + angle));
    }
}