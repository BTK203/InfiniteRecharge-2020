/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.auto;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.RunCommand;
import frc.robot.Constants;
import frc.robot.commands.ConstantCommandDriveIntake;
import frc.robot.commands.CyborgCommandAlignTurret;
import frc.robot.commands.CyborgCommandDriveDistance;
import frc.robot.commands.CyborgCommandSetTurretPosition;
import frc.robot.commands.CyborgCommandShootPayload;
import frc.robot.commands.CyborgCommandSmartDriveDistance;
import frc.robot.commands.CyborgCommandWait;
import frc.robot.subsystems.SubsystemDrive;
import frc.robot.subsystems.SubsystemFeeder;
import frc.robot.subsystems.SubsystemFlywheel;
import frc.robot.subsystems.SubsystemIntake;
import frc.robot.subsystems.SubsystemReceiver;
import frc.robot.subsystems.SubsystemTurret;
import frc.robot.util.Util;

/**
 * A slightly cooler auto than it's other simpler counterparts.
 * This auto will back off the line while zeroing the turret, and 
 * then slowly drive backwards, collecting power cells from the trench while shooting power
 * cells already loaded in.
 */
public class SixBallSimpleAuto implements IAuto {
    private Command
        init,
        positionTurret,
        alignTurret,
        shootTwoBalls,
        driveBack,
        collectBalls,
        wait,
        driveForward,
        alignAgain,
        shootPayload,
        declareVictory; //possibly temporary, just indicates to driverstation that command is done 


    /**
     * Creates a new SixBallSimpleAuto, initalizing commands
     */
    public SixBallSimpleAuto (
        SubsystemDrive drivetrain,
        SubsystemTurret turret,
        SubsystemReceiver kiwilight,
        SubsystemIntake intake,
        SubsystemFeeder feeder,
        SubsystemFlywheel flywheel
    ) {
        this.init = new InitAuto(drivetrain, turret).getCommand();
        
        //position turret to get target in vision view
        int yawTarget = Auto.getYawTicksToTarget(Util.getAndSetDouble("Auto Start Offset", 0));
        int pitchTarget = Constants.AUTO_INIT_PITCH_TARGET;
        this.positionTurret = new CyborgCommandSetTurretPosition(turret, yawTarget, pitchTarget, true, kiwilight, false);
        
        //align
        this.alignTurret = new CyborgCommandAlignTurret(turret, kiwilight, true);
        this.shootTwoBalls = new CyborgCommandShootPayload(intake, feeder, flywheel, turret, 2, 15000, false);
        
        double trenchDistance = (double) Constants.AUTO_SHALLOW_TRENCH_DISTANCE;
        //init drive commands. Use CyborgCommandSmartDriveDistance if NavX is connected
        if(drivetrain.getNavXConnected() && Util.getAndSetBoolean("Use SmartDistance", true)) {
            this.driveBack = new CyborgCommandSmartDriveDistance(drivetrain, trenchDistance, Constants.DRIVE_AUTO_INHIBITOR);
            this.driveForward = new CyborgCommandSmartDriveDistance(drivetrain, trenchDistance * -1, Constants.DRIVE_AUTO_INHIBITOR);
        } else {
            this.driveBack = new CyborgCommandDriveDistance(drivetrain, trenchDistance, Constants.DRIVE_AUTO_INHIBITOR);
            this.driveForward = new CyborgCommandDriveDistance(drivetrain, trenchDistance * -1, Constants.DRIVE_AUTO_INHIBITOR);
        }
        this.collectBalls = new ConstantCommandDriveIntake(intake, feeder);
        this.wait = new CyborgCommandWait(Constants.TRENCH_AUTO_WAIT_TIME);

        //shoot balls
        this.alignAgain = new CyborgCommandAlignTurret(turret, kiwilight, true);
        this.shootPayload = new CyborgCommandShootPayload(intake, feeder, flywheel, turret, 4, 15000, false);
        
        //tell DS that we done
        this.declareVictory = new RunCommand(
            () -> {
                DriverStation.reportWarning("6-BALL DONE", false);
            }
        );
    }

    public Command getCommand() {
        Command initAndShoot = init.andThen(positionTurret, alignTurret, shootTwoBalls);
        Command driveAndCollect = (driveBack.andThen(wait, driveForward)).raceWith(collectBalls);
        return initAndShoot.andThen(driveAndCollect, alignAgain, shootPayload, declareVictory);
    }

    public boolean requiresFlywheel() {
        return true;
    }
}
