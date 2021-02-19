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
import frc.robot.util.PathRecorder;
import frc.robot.util.Point2D;
import frc.robot.util.Util;

public class CyborgCommandEmulatePath extends CommandBase {
  private SubsystemDrive drivetrain;
  private Point2D[] points;
  private int currentPointIndex;
  private long lastTime;
  private double
    lastBestSpeed,
    lastLeftVelocity,
    lastRightVelocity;
  private boolean isForwards;
  private PathRecorder recorder;

  /** Creates a new CyborgCommandEmulatePath. */
  public CyborgCommandEmulatePath(SubsystemDrive drivetrain) {
    this.drivetrain = drivetrain;
    recorder = new PathRecorder("/home/lvuser/results.txt");

    addRequirements(drivetrain);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    currentPointIndex = 1;
    recorder.init();
    lastTime = System.currentTimeMillis();
    lastBestSpeed = 0;
    lastLeftVelocity = 0;
    lastRightVelocity = 0;

    try {
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
    Robot.getRobotContainer().zeroAllDrivetrain();

    isForwards = new Point2D(0, 0, 0).getHeadingTo(points[1]) < 90;
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    Point2D currentLocation = Robot.getRobotContainer().getRobotPositionAndHeading();
    recorder.recordPoint(currentLocation);

    //resolve the point that the robot is currently at and where we want to aim
    if(currentPointIndex < points.length - 1) {
      double currentDirection = forwardsify(currentLocation.getHeading());
      for(int limit=0; limit<Constants.EMULATE_POINT_SKIP_LIMIT; limit++) {
        //get the angle that the root needs to turn to acheive the point
        SmartDashboard.putNumber("Emulate heading current to next", currentLocation.getHeadingTo(points[currentPointIndex]));
        double headingToNext = Math.abs(Util.getAngleToHeading(currentDirection, currentLocation.getHeadingTo(points[currentPointIndex])));
        SmartDashboard.putNumber("Emulate Heading to next", headingToNext);

        //get a path that consists of future points. If they are straight, 
        if(currentPointIndex < points.length - 1 && headingToNext >= 75) {
          currentPointIndex++;

          DriverStation.reportError("current heading: " + Double.valueOf(currentDirection).toString() + ", heading to: " + Double.valueOf(currentLocation.getHeadingTo(points[currentPointIndex])).toString() + ", heading to next: " + Double.valueOf(headingToNext).toString(), false);
        } else {
          break;
        }
      }
    }

    currentPointIndex = (currentPointIndex > points.length - 2 ? points.length - 2 : currentPointIndex);

    SmartDashboard.putNumber("Emulate current point", currentPointIndex);
    SmartDashboard.putNumber("Emulate points", points.length);
    SmartDashboard.putString("Emulate target point", points[currentPointIndex].toString());

    Point2D currentDestination = points[currentPointIndex + 1];

    //figure out if we need to drive forwards or backwards to acheive the point
    double headingToNextPoint = currentLocation.getHeadingTo(currentDestination);
    double headingDifference = Util.getAngleToHeading(currentLocation.getHeading(), headingToNextPoint); //add this back in if the new code breaks
    SmartDashboard.putNumber("Emulate heading diff", headingDifference);
    // double headingDifference = Util.getAngleToHeading(currentLocation.getHeading(), currentDestination.getHeading());
    this.isForwards = Math.abs(headingDifference) < 90;

    SmartDashboard.putBoolean("Emulate Forward", isForwards);

    //Resolve the path of points that are immediately ahead of the robot. This array will include the robot's location as the first point.
    int immediatePathSize = (int) Util.getAndSetDouble("Emulate Immediate Path Size", 5);
    Point2D[] nextPoints = getNextNPoints(points, currentPointIndex + 2, immediatePathSize);
    Point2D[] immediatePath = new Point2D[nextPoints.length + 1];

    //set first point to robot location, but the heading must be forwards trajectory.
    immediatePath[0] = new Point2D(currentLocation.getX(), currentLocation.getY(), forwardsify(currentLocation.getHeading()));
    for(int i=1; i<immediatePath.length; i++) {
      immediatePath[i] = nextPoints[i - 1];
    }

    //draw an "arc" that closely fits the path. The arc will be used to calculate the left and right velocities.
    double immediateDistance = getDistanceOfPath(immediatePath); //unit: in
    double immediateTurn = getTurnOfPath(immediatePath); //unit: degrees
    double headingChange = Util.getAngleToHeading(immediatePath[1].getHeading(), immediatePath[immediatePath.length - 1].getHeading());
    SmartDashboard.putNumber("Emulate heading change", headingChange);

    //figure out if the robot should switch directions (forward to backward or vice versa) without changing heading.
    double turnToHeadingDifference = Math.abs(Util.getAngleToHeading(headingChange, immediateTurn));
    SmartDashboard.putNumber("Emulate Turn to heading difference", turnToHeadingDifference);
    boolean shouldZeroTurn = turnToHeadingDifference > Constants.EMULATE_MAX_HEADING_TO_TURN_DIFFERENCE;
    // boolean shouldZeroTurn = Math.abs(immediateTurn) > 165;
    

    SmartDashboard.putNumber("Emulate Immediate Turn Before Overturn", immediateTurn);

    if(immediateTurn < Util.getAndSetDouble("Positional Correction Threshold", 20)) {
      //add positional correction to heading by aiming for 2 points ahead of us
      Point2D targetPoint = points[currentPointIndex + 2];
      double positionalCorrection = Util.getAngleToHeading(forwardsify(currentLocation.getHeading()), currentLocation.getHeadingTo(targetPoint));
      positionalCorrection *= currentLocation.getDistanceFrom(targetPoint) * Util.getAndSetDouble("Emulate Positional Correction Inhibitor", 1);
      immediateTurn += positionalCorrection;
      immediateTurn *= Util.getAndSetDouble("Emulate Overturn", 1.2);
    }

    //EXPERIMENTAL AND NOT PERM.
    if(!isForwards) {
      immediateTurn *= 2;
    }

    SmartDashboard.putNumber("Emulate immediate turn", immediateTurn);
    immediateTurn = Math.toRadians(immediateTurn); //we need radians for arc length    

    if(immediateTurn != 0) {
      //use immediateDistance and immediateTurn to calculate the left and right base velocities of the wheels.
      double radius = immediateDistance / immediateTurn; //unit: in

      double leftDisplacement = 0;
      double rightDisplacement = 0;

      double baseSpeed = calculateBestTangentialSpeed(radius);
      SmartDashboard.putNumber("Calc. Emulate Base Speed", baseSpeed);

      if(isForwards) {
        leftDisplacement  = immediateTurn * (radius - (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2)); //unit: in
        rightDisplacement = immediateTurn * (radius + (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2));
      } else {
        leftDisplacement  = -1 * immediateTurn * (radius + (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2)); //unit: in
        rightDisplacement = -1 * immediateTurn * (radius - (Constants.DRIVETRAIN_WHEEL_BASE_WIDTH / 2));
      }

      SmartDashboard.putNumber("Emulate left displacement", leftDisplacement);
      SmartDashboard.putNumber("Emulate right displacement", rightDisplacement);

      if (
        (leftDisplacement < 0 && rightDisplacement < 0 && isForwards) ||
        (leftDisplacement > 0 && rightDisplacement > 0 && !isForwards)
      ) {
        leftDisplacement *= -1;
        rightDisplacement *= -1;
      }

      //convert displacments to velocities
      double timeInterval  = immediateDistance / baseSpeed; // unit: sec
      double leftVelocity  = leftDisplacement / timeInterval; //unit: in/sec
      double rightVelocity = rightDisplacement / timeInterval;

      SmartDashboard.putNumber("Emulate left vel.", leftVelocity);
      SmartDashboard.putNumber("Emulate right vel.", rightVelocity);

      //occasionally, the drivetrain will fail to reach a point and start driving in the other direction.
      //This code fixes that problem.

      if(shouldZeroTurn) {
        double vel = (isForwards ? baseSpeed : -1 * baseSpeed);
        leftVelocity = vel;
        rightVelocity = vel;
      }

      leftVelocity = IPStoRPM(leftVelocity);
      rightVelocity = IPStoRPM(rightVelocity);
      
      leftVelocity = curveVelocity(leftVelocity);
      rightVelocity = curveVelocity(rightVelocity);

      drivetrain.setLeftVelocity(leftVelocity);
      drivetrain.setRightVelocity(rightVelocity);
    } else {
      double baseSpeed = Util.getAndSetDouble("Emulate Max Speed", 40);
      if(!isForwards) {
        baseSpeed *= -1;
      }

      baseSpeed = ramp(baseSpeed, lastBestSpeed, Util.getAndSetDouble("Emulate Ramp", 0.75));
      lastBestSpeed = baseSpeed;

      double curvedBaseSpeed = curveVelocity(IPStoRPM(baseSpeed));
      drivetrain.setLeftVelocity(curvedBaseSpeed);
      drivetrain.setRightVelocity(curvedBaseSpeed);
    }

    lastTime = System.currentTimeMillis();
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    drivetrain.setLeftPercentOutput(0);
    drivetrain.setRightPercentOutput(0);
    recorder.closeFile();
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return currentPointIndex >= points.length - 3; //command will finish when the last point is acheived.
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

  /**
   * Returns the sum of the distance between all points of a path.
   * @param path An array of points representing the path.
   * @return The approximate distance of the path.
   */
  private double getDistanceOfPath(Point2D[] path) {
    double distance = 0;
    for(int i=0; i<path.length - 1; i++) {
      distance += path[i].getDistanceFrom(path[i + 1]);
    }

    return distance;
  }
  
  /**
   * Converts a velocity in inches/sec to RPM.
   * @param ips A velocity in inches/sec
   * @return A velocity in RPM that corresponds to the velocity in ips.
   */
  private double IPStoRPM(double ips) {
    double newVelocity = ips * Constants.DRIVE_ROTATIONS_PER_INCH; //convert to rotations per second
    newVelocity *= 60; //convert to rotations per minute
    return newVelocity;
  }

  /**
   * This odd method is here so that the robot can achieve almost any velocity using only one set of PID constants.
   * It works by increasing the target velocity so that the PID is forced to work harder than it would work otherwise.
   * @param velocitySetpoint The original velocity setpoint in RPM
   * @return The curved velocity setpoint in RPM
   */
  private double curveVelocity(double velocitySetpoint) {
    return (velocitySetpoint > 1132 ? velocitySetpoint += (velocitySetpoint - 40) * 0.4 : velocitySetpoint); //1132 RPM ~= 45 in/sec
  }

  /**
   * Returns the average turn of a path.
   * @param path An array of points representing the path.
   * @return The average turn of the path in degrees.
   */
  private double getTurnOfPath(Point2D[] path) {
    double turn = 0;
    double lastHeading = path[0].getHeading();
    for(int i=1; i<path.length; i++) {
      double headingToPoint = path[i - 1].getHeadingTo(path[i]);
      double correctionToPoint = Util.getAngleToHeading(lastHeading, headingToPoint);

      turn += correctionToPoint;
      lastHeading = headingToPoint;
    }

    return turn;
  }

  /**
   * Returns the change in heading the robot will experience through a path.
   * @param path The path immediately in front of the robot. Ideally, the first point of the path should be the robot's position and heading.
   * @return The heading change the robot will experience through the path in degrees.
   */
  private double getHeadingChangeOfPath(Point2D[] path) {
    SmartDashboard.putNumber("Emulate path 0 heading", path[0].getHeading());
    SmartDashboard.putNumber("Emulate path length heading", path[path.length - 1].getHeading());
    return Util.getAngleToHeading(path[0].getHeading(), path[path.length - 1].getHeading());
  }

  /**
   * Returns an angle corresponding to the direction that the robot is travelling in
   * @param angle Original angle.
   * @param isForwards True if robot is driving forwards, false otherwise
   */
  private double forwardsify(double angle) {
    return (isForwards ? angle : (angle + 180) % 360);
  }

  /**
   * Calculates the best speed that the robot should drive through an arc at.
   * @param turnRadius The radius of the turn that the robot will take in inches.
   * @return The best speed for the turn in in/sec
   */
  private static double calculateBestTangentialSpeed(double turnRadius) {
    double maxSpeed = Util.getAndSetDouble("Emulate Max Speed", 60);
    double minSpeed = Util.getAndSetDouble("Emulate Min Speed", 50);
    if(Double.isNaN(turnRadius)) {
      return maxSpeed;
    }

    //gather needed variables (coefficient of friction, normal force, and mass) and convert to SI units.
    double coefficientOfFriction = Util.getAndSetDouble("Emulate Coefficient of Friction", 1); //defaults to the approximate CoE of rubber on concrete. No Unit.
    double normalForce = Util.poundForceToNewtons(Constants.ROBOT_WEIGHT_POUND_FORCE); //unit: N
    double robotMass   = Util.weightLBFToMassKG(Constants.ROBOT_WEIGHT_POUND_FORCE); //unit: kg
    double radius      = Math.abs(Util.inchesToMeters(turnRadius)); //unit: m. We can absolute value it because we dont care about the direction of the arc.

    //formula: v = sqrt( (r * CoE * Fn) / m )
    double bestSpeed = Math.sqrt( ( radius * coefficientOfFriction * normalForce ) / robotMass ); //unit: m/s

    //convert best speed to in/s
    bestSpeed = Util.metersToInches(bestSpeed); //unit: in/s
    bestSpeed = (bestSpeed > maxSpeed ? maxSpeed : (bestSpeed < minSpeed ? minSpeed : bestSpeed));

    SmartDashboard.putNumber("Emulate bs radius", radius);
    SmartDashboard.putNumber("Emulate turn radius", turnRadius);
    SmartDashboard.putNumber("Emulate best speed", bestSpeed);
    SmartDashboard.putNumber("Emulate Normal Force", normalForce);
    SmartDashboard.putNumber("Emulate Robot Mass", robotMass);
    SmartDashboard.putNumber("Emulate CoE", coefficientOfFriction);

    return bestSpeed;
  }


  /**
   * Ramps a value.
   * @param power desired value
   * @param last last value
   * @param ramp desired ramp (seconds from 0 to 1)
   * @return ramped value from -1 to 1
   */
  private double ramp(double power, double last, double ramp) {
    double rampedPower = power;
    long currentTime = System.currentTimeMillis();

    double elapsedTime = (currentTime - lastTime) / 1000.0;
    double rampMultiplier = elapsedTime / ramp;
    rampMultiplier = (rampMultiplier > 1 ? 1 : rampMultiplier);

    DriverStation.reportError("power: " + Double.valueOf(power).toString(), false);
    DriverStation.reportError("last: " + Double.valueOf(last).toString(), false);

    rampedPower = last + (power * rampMultiplier);

    DriverStation.reportError("new power: " + Double.valueOf(rampedPower).toString(), false);

    return rampedPower;
  }

  //TESTING
  /**
   * Prints test values. This method is temporary.
   */
  public boolean test() {
    return true;
  }
}
