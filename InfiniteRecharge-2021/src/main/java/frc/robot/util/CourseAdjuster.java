// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.util;

import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;
import frc.robot.subsystems.SubsystemDrive;

/**
 * The new 2021 driver. Yee haw.
 */
public class CourseAdjuster {
    private SubsystemDrive drivetrain;
    private PIDController headingController;
    private double
        targetHeading,
        targetVelocity;
        
    /**
     * Creates a new CourseAdjuster
     * @param drivetrain Drivetrain of the robot.
     * @param headingkP The P value of the heading loop.
     * @param headingkI The I value of the heading loop.
     * @param headingkD The D value of the heading loop.
     * 
     * NOTE: This class does not set the drivetrain PID values in this constructor.
     * Drivetrain PID values can be set using setDrivetrainPIDF() or setting the values on the drivetrain.
     * Also, the init() method must be called before running. Call this in the initialize() method of
     * the command.
     */
    public CourseAdjuster(SubsystemDrive drivetrain, double headingkP, double headingkI, double headingkD) {
        this.drivetrain = drivetrain;
        this.headingController = new PIDController(headingkP, headingkI, headingkD);
    }

    /**
     * Sets the PID constants on the drivetrain.
     */
    public void setDrivetrainPIDF(double kP, double kI, double kD, double kF, double iZone, double lowOutLimit, double highOutLimit) {
        drivetrain.setPIDConstants(kP, kI, kD, kF, iZone, lowOutLimit, highOutLimit);
    }

    /**
     * Initializes / resets the object.
     * This should be called in the initialize() method of the command invoking this object.
     */
    public void init() {
        this.headingController.setSetpoint(drivetrain.getGyroAngle()); //set default heading to current heading
    }

    /**
     * Updates the object.
     * This should be called in the update() method of the command invoking this object.
     */
    public void update() {
        double velocitySetpoint = targetVelocity;
        SmartDashboard.putNumber("Test Velocity Setpoint", velocitySetpoint);
        if(velocitySetpoint > 1132) velocitySetpoint += (velocitySetpoint - 40) * 0.4; //1132 RPM ~= 45 in/sec

        //correct heading
        headingController.setSetpoint(targetHeading);
        double headingCorrection = headingController.calculate(drivetrain.getGyroAngle());
        double currentVelocity = drivetrain.getOverallVelocity();
        headingCorrection *= (currentVelocity) / 2;
        double leftVelocity  = velocitySetpoint - headingCorrection;
        double rightVelocity = velocitySetpoint + headingCorrection;

        drivetrain.setLeftVelocity(leftVelocity);
        drivetrain.setRightVelocity(rightVelocity);
    }

    /**
     * Sets the target heading of the robot.
     */
    public void setHeading(double heading) {
        targetHeading = heading;
    }

    /**
     * Sets the velocity of the robot.
     */
    public void setVelocity(double velocity) {
        double newVelocity = velocity * Constants.DRIVE_ROTATIONS_PER_INCH; //convert to rotations per second
        newVelocity *= 60; //convert to rotations per minute
        this.targetVelocity = newVelocity;
    }

    /**
     * Updates the PID Constants for heading correction.
     * @param kP The P (proportional) value.
     * @param kI The I (integral) value.
     * @param kD The D (derivative) value.
     */
    public void setHeadingPID(double kP, double kI, double kD) {
        headingController.setP(kP);
        headingController.setI(kI);
        headingController.setD(kD);
    }
}
