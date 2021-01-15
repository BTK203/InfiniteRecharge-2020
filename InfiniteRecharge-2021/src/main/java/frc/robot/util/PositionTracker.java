// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.util;

import edu.wpi.first.wpilibj.DriverStation;
import frc.robot.subsystems.SubsystemDrive;

/** Add your docs here. */
public class PositionTracker {
    private SubsystemDrive drivetrain;
    private double
        lastLeftDistance,
        lastRightDistance,
        x,
        y,
        heading;

    /**
     * Creates a new PositionTracker.
     * @param x The starting X-coordinate of the robot.
     * @param y The starting Y-coordinate of the robot.
     * @param angle The starting heading angle of the robot in degrees. (0 = towards positive X. Positive = CCW)
     */
    public PositionTracker(SubsystemDrive drivetrain, double x, double y, double heading) {
        this.drivetrain = drivetrain;
        this.x = x;
        this.y = y;
        this.heading = heading;

        //track robot position in new thread
        new Thread(
            () -> {
                //TODO: decide if we want to keep loop time tracking
                long lastLoopTime = System.currentTimeMillis();
                while(true) {
                    //update using drivetrain values.
                    update();

                    //track and report time elapsed during update.
                    long currentTime = System.currentTimeMillis();
                    long iterationTime = currentTime - lastLoopTime;
                    DriverStation.reportWarning("PositionTracker iteration time: " + Long.valueOf(iterationTime).toString(), false);
                    lastLoopTime = currentTime;
                }
            }
        ).start();
    }

    /**
     * Creates a new Position tracker, with starting position and rotation at 0.
     */
    public PositionTracker(SubsystemDrive drivetrain) {
        this(drivetrain, 0, 0, 0);
    }

    /**
     * Sets the position and heading of the robot.
     * @param x The new X-coordinate of the robot.
     * @param y The new Y-coordinate of the robot.
     * @param angle The new heading angle of the robot.
     */
    public void setPositionAndHeading(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    /**
     * Updates the position of the robot using a distance travelled and heading travelled in.
     * @param driveDistance The average of the drive distance of the two sides of the drivetrain.
     * @param rotation The current rotation of the robot.
     */
    public void update(double driveDistance, double rotation) {
        double averageHeading = (rotation + this.heading) / 2;

        //break vector into components
        double driveX = driveDistance * Math.cos(averageHeading);
        double driveY = driveDistance * Math.sin(averageHeading);

        this.x += driveX;
        this.y += driveY;
    }

    /**
     * Updates the position of the robot using values from the drivetrain.
     */
    public void update() {
        double currentLeftDistance = drivetrain.getLeftPosition();
        double currentRightDistance = drivetrain.getRightPosition();
        double leftChange = currentLeftDistance - lastLeftDistance;
        double rightChange = currentRightDistance - lastRightDistance;
        
        //take average to get average distance travelled by the center of the bot
        double netDistanceTravelled = (leftChange + rightChange) / 2;
        double currentHeading = drivetrain.getGyroAngle();

        //update using new values
        update(netDistanceTravelled, currentHeading);
    }

    public Point2D getPositionAndHeading() {
        return new Point2D(x, y, heading);
    }
}
