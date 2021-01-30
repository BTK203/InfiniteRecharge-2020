// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Constants;
import frc.robot.Robot;
import frc.robot.subsystems.SubsystemDrive;
import frc.robot.util.CourseAdjuster;
import frc.robot.util.Point2D;
import frc.robot.util.TrajectorySegment;
import frc.robot.util.Util;

public class CyborgCommandEmulatePath extends CommandBase {
  private CourseAdjuster courseAdjuster;
  private SubsystemDrive drivetrain;
  private Point2D[] points;
  private int currentPointIndex;
  private TrajectorySegment currentTrajectory;
  private double
    currentSegmentTravel,
    lastRightPosition,
    lastLeftPosition;

  /** Creates a new CyborgCommandEmulatePath. */
  public CyborgCommandEmulatePath(SubsystemDrive drivetrain) {
    this.drivetrain = drivetrain;
    addRequirements(drivetrain);

    this.courseAdjuster = new CourseAdjuster(drivetrain, 0, 0, 0);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    currentTrajectory = new TrajectorySegment(0, 0, -1); //distance to -1 so that this segment will always be overwritten on first execute()
    currentSegmentTravel = 0;
    lastRightPosition = drivetrain.getRightPosition();
    lastLeftPosition  = drivetrain.getLeftPosition();
    currentPointIndex = 0;

    try {
      // String fileContents = Files.readString(Path.of("D:\\_Users\\Brach\\projects\\Test\\FRC\\out.txt")); //TODO: DELETE
      String fileContents = Files.readString(Path.of("/home/lvuser/points.txt"));

      //create array of points based on fileContents
      String pointStrings[] = fileContents.split("\n");
      points = new Point2D[pointStrings.length];
      for(int i=0; i<pointStrings.length; i++) {
        points[i] = Point2D.fromString(pointStrings[i]);
      }
    } catch (IOException ex) {
      DriverStation.reportError("IO EXCEPTION", true);
    }

    //update the PID Constants for heading.
    double 
      kP           = Util.getAndSetDouble("Drive Velocity kP", 0),
      kI           = Util.getAndSetDouble("Drive Velocity kI", 0),
      kD           = Util.getAndSetDouble("Drive Velocity kD", 0),
      kF           = Util.getAndSetDouble("Drive Velocity kF", 0),
      izone        = Util.getAndSetDouble("Drive Velocity IZone", 0),
      outLimitLow  = Util.getAndSetDouble("Drive Velocity Out Limit Low", -1),
      outLimitHigh = Util.getAndSetDouble("Drive Velocity Out Limit High", 1);

    // //drivetrain closed loop ramp
    drivetrain.setPIDRamp(Util.getAndSetDouble("Drive PID Ramp", 0.5));
    drivetrain.setPIDConstants(kP, kI, kD, kF, izone, outLimitLow, outLimitHigh);
    
    //update the PID Constansts for heading.
    double
      headingkP = Util.getAndSetDouble("Drive Heading kP", 0),
      headingkI = Util.getAndSetDouble("Drive Heading kI", 0),
      headingkD = Util.getAndSetDouble("Drive Heading kD", 0),
      headingInhibitor = Util.getAndSetDouble("Course Heading Correction Inhibitor", 0.25);

    courseAdjuster.setHeadingPID(headingkP, headingkI, headingkD);
    courseAdjuster.setHeadingCorrectionInhibitor(headingInhibitor);
    courseAdjuster.setTurn(0);
    courseAdjuster.init();

    Robot.getRobotContainer().zeroAllDrivetrain();
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    //Determine the distance we have travelled while on the current trajectory.
    double currentRightPosition = drivetrain.getRightPosition();
    double currentLeftPosition  = drivetrain.getLeftPosition();

    double rightDisplacement = currentRightPosition - lastRightPosition;
    double leftDisplacement  = currentLeftPosition - lastLeftPosition;
    double netDisplacement   = (rightDisplacement + leftDisplacement) / 2;

    currentSegmentTravel += netDisplacement;

    //set history
    lastRightPosition = currentRightPosition;
    lastLeftPosition  = currentLeftPosition;

    //overwrite the current trajectory if necessary.
    if(currentSegmentTravel > currentTrajectory.getDistance()) {
      currentPointIndex++;
      Point2D currentPosition = Robot.getRobotContainer().getRobotPositionAndHeading();
      Point2D destination = points[currentPointIndex];
      double baseSpeed = Util.getAndSetDouble("Emulation Base Speed", 75);
      currentTrajectory = calculateTrajectory(currentPosition, destination, baseSpeed);

      currentSegmentTravel = 0;
    }

    courseAdjuster.setBaseLeftVelocity(currentTrajectory.getLeftVelocity());
    courseAdjuster.setBaseRightVelocity(currentTrajectory.getRightVelocity());
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    drivetrain.setLeftPercentOutput(0);
    drivetrain.setRightPercentOutput(0);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return currentPointIndex >= points.length - 2; //command will finish when the last point is acheived.
  }

  /**
   * Returns an "n" long array of points, starting at start.
   * @param baseArray The array to create a sub-array from.
   * @param start     The index to start the sub-array from.
   * @param n         The length of the sub-array.
   * @return An "n" long array of Point2D objects. May be shorter if forbidden indices exist (start + n > length).
   */
  private Point2D[] getNextNPoints(Point2D[] baseArray, int start, int n) {
    int end = start + n;
    end = (end > baseArray.length ? baseArray.length : end);

    Point2D[] points = new Point2D[end - start];
    for(int i=start; i<end; i++) {
      points[i - start] = baseArray[i];
    }

    return points;
  }

  private double getDistanceOfPath(Point2D[] path) {
    double distance = 0;
    for(int i=0; i<path.length - 1; i++) {
      distance += path[i].getDistanceFrom(path[i + 1]);
    }

    SmartDashboard.putNumber("Emulate Mini Path length", path.length);
    SmartDashboard.putNumber("Emulate Path Distance", distance);

    return distance;
  }

  /**
   * Calculates the left and right velocities required for the robot to acheive a destination and heading, and returns
   * it along with a distance that the robot needs to drive with those velocities.
   * @param currentPosition The current position and heading of the robot's center.
   * @param destination The new desination (with heading) of the robot's center.
   * @return A TrajectorySegment, which contains the left velocty, right velocity, and required distance to acheive the desination.
   */
  private TrajectorySegment calculateTrajectory(Point2D currentPosition, Point2D destination, double baseSpeed) {
    //to calculate the required velocities, first derive the radius of the required arc from the two points.
    //to derive the radius, we need the distance between the points and the heading to the desination.
    boolean isForwards = Math.abs(currentPosition.getHeadingTo(destination)) < 90;

    double turn = Util.getAngleToHeading(currentPosition.getHeading(), destination.getHeading());
    double distanceToDestination = currentPosition.getDistanceFrom(destination); //not what will be returned. This is a birds-eye value. Unit: in

    if(turn == 0) {
      return new TrajectorySegment(baseSpeed, baseSpeed, distanceToDestination);
    }

    double headingToDestination  = currentPosition.getHeadingTo(destination);

    double a = 90 - destination.getHeading() - headingToDestination; //this value represents the angle between the line through both points (current and dest.) and the radius. It is named "a" because that description doesn't make a good name.
    double c = 180 - (2 * a); //represents the angle between the radii of the endpoints of the arc we are making.

    //for the remainder of calcuations, the angles must be in radians.
    a = Math.toRadians(a);
    c = Math.toRadians(c);

    //use the Law of Sines to derive the radius of the arc.
    double radius = (distanceToDestination * Math.sin(a)) / Math.sin(c); //unit: in

    //now use angular kinematics to determine the velocities of both sides of the drivetrain.
    double arcDistance = radius * c; //unit: in

    double leftDisplacement = 0;
    double rightDisplacement = 0;

    if(isForwards) {
      leftDisplacement  = turn * (radius - (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2)); //unit: in
      rightDisplacement = turn * (radius + (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2));
    } else {
      leftDisplacement  = -1 * turn * (radius + (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2)); //unit: in
      rightDisplacement = -1 * turn * (radius - (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2));
    }

    //convert displacments to velocities
    double timeInterval  = arcDistance / baseSpeed; // unit: sec
    double leftVelocity  = leftDisplacement / timeInterval; //unit: in/sec
    double rightVelocity = rightDisplacement / timeInterval;

    return new TrajectorySegment(leftVelocity, rightVelocity, arcDistance);
  }
}
