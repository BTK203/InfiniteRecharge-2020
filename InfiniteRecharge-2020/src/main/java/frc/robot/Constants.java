/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import frc.robot.util.Point3D;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide
 * numerical or boolean constants. This class should not be used for any other
 * purpose. All constants should be declared globally (i.e. public static). Do
 * not put anything functional in this class.
 *
 * <p>
 * It is advised to statically import this class (or one of its inner classes)
 * wherever the constants are needed, to reduce verbosity.
 */
public final class Constants {

    /**
     * Drivetrain Motor IDs
     */
    public static final int
        DRIVE_RIGHT_MASTER_ID = 3, //spark
        DRIVE_RIGHT_SLAVE_ID  = 4, //spark
        DRIVE_LEFT_MASTER_ID  = 1, //spark
        DRIVE_LEFT_SLAVE_ID   = 2; //spark

    /**
     * Turret Motor IDs
     */
    public static final int
        TURRET_YAW_ID      = 8,
        TURRET_PITCH_ID    = 9,
        TURRET_FLYWHEEL_ID = 7; //spark

    /**
     * Climber Motor IDs
     */
    public static final int
        CLIMBER_SCISSOR_ID = 5, //spark
        CLIMBER_WINCH_ID = 6;   //spark

    /**
     * Spinner Motor IDs
     */
    public static final int
        SPINNER_ID = 10;

  /**
   * Intake IDs
   */
    public static final int
        MAININTAKE_ID = 0,
        FEEDINTAKE_ID = 0;
    
    /**
     * Intake Motor IDs
     */
    public static final int
        EATER_ID = 11,
        SLAPPER_ID = 12;

    /**
     * Feeder Motor IDs
     */
    public static final int
        BEATER_ID = 13,
        FEEDER_ID = 14;
    
    /**
     * Drivetrain motor inverts
     */
    public static final boolean
        DRIVE_RIGHT_MASTER_INVERT = false,
        DRIVE_RIGHT_SLAVE_INVERT  = false,
        DRIVE_LEFT_MASTER_INVERT  = true,
        DRIVE_LEFT_SLAVE_INVERT   = true;

    /**
     * Turret Motor Inverts
     */
    public static final boolean
        TURRET_YAW_INVERT      = false,
        TURRET_PITCH_INVERT    = false,
        TURRET_FLYWHEEL_INVERT = true;

    /**
     * Climber Inverts
     */
    public static final boolean
        CLIMBER_SCISSOR_INVERT = false,
        CLIMBER_WINCH_INVERT   = true;

    /**
     * Spinner Inverts
     */
    public static final boolean
        SPINNER_INVERT = false;

    /**
     * Intake Motor Inverts
     */
    public static final boolean
        EATER_INVERT   = false,
        SLAPPER_INVERT = false;

    /**
     * Feeder Motor Inverts
     */
    public static final boolean
        BEATER_INVERT = false,
        FEEDER_INVERT = false;

    /**
     * Braking Values
     */
    public static final boolean
        INTAKE_BRAKING = true,
        FEEDER_BRAKING = true;

    /**
     * Amp Limits
     */
    public static int
        FLYWHEEL_AMP_LIMIT = 50;
    
    /*
    * Climber Values
    */
    public static final double
        LOWEST_HEIGHT = 0,
        ON_WHEEL_HEIGHT = 30,
        ABOVE_WHEEL_HEIGHT = 32,
        HIGHEST_HEIGHT = 60;
        
    /*
    * FORMAT: Red_Min, Green_Min, Blue_Min, Red_Max, Green_Max, Blue_Max.
    */
    public static final int[]
        TARGET_RED    = { 99 , 90 , 35 , 119, 110, 55  },
        TARGET_GREEN  = { 42 , 126, 56 , 62 , 146, 76  },
        TARGET_BLUE   = { 36 , 103, 85 , 56 , 123, 105 },
        TARGET_YELLOW = { 75 , 127, 22 , 95 , 147, 42  };
    
    /**
     * Extraneous values
     */
    public static final int
        SPINNER_SPEED = 1,
        TURRET_YAW_DEGREES = 345,
        TURRET_PITCH_DEGREES = 100,
        DEFAULT_TURRET_YAW_TICKS = 1615628,
        DEFAULT_TURRET_PITCH_TICKS = 1000,
        FLYWHEEL_STABLE_RPM = 5750,
        TURRET_YAW_ALLOWABLE_ERROR = 500,
        TURRET_PITCH_ALLOWABLE_ERROR = 5,
        TURRET_CENTER_ANGLE_OFFSET_YAW = 90,
        TURRET_PITCH_CAMERA_OFFSET = 25;

    public static final double 
        FLYWHEEL_GEAR_RATIO = 1.6071,
        DRIVETRAIN_WHEEL_DIAMETER = 7, //in
        DRIVETRAIN_ALLOWABLE_ERROR = 0.0625,
        TURRET_HEIGHT = 24,
        TURRET_YAW_DEGREES_AT_ZERO = -90, //degrees from zero to turret looking forward
        TURRET_PITCH_DEGREES_AT_ZERO = 5;

    public static final boolean
        DRIVE_CAMERA_AUTOMATIC_EXPOSURE = true;

    /**
     * Auto values
     */
    public static final Point3D
        POWERPORT_LOCATION = new Point3D(94.66, 0, 122.25);

    public static final int
        AUTO_INIT_BALL_COUNT = 3,
        AUTO_PAYLOAD_TIMEOUT = 3000, //ms
        DISTANCE_INIT_LINE_TO_ALLIANCE_WALL = 120,
        KIWILIGHT_STABLE_DEGREES = 1; //in
}
