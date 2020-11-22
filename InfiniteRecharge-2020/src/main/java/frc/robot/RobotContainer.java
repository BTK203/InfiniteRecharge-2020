/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import frc.robot.auto.BareMinimumAuto;
import frc.robot.auto.IAuto;
import frc.robot.auto.InitAuto;
import frc.robot.auto.SixBallSimpleAuto;
import frc.robot.auto.TrenchAuto;
import frc.robot.commands.ButtonCommandGroupRunIntakeFeeder;
import frc.robot.commands.ButtonCommandMoveClimber;
import frc.robot.commands.CyborgCommandAlignTurret;
import frc.robot.commands.CyborgCommandCalibrateTurretPitch;
import frc.robot.commands.CyborgCommandCalibrateTurretYaw;
import frc.robot.commands.CyborgCommandDriveDistance;
import frc.robot.commands.CyborgCommandFlywheelVelocity;
import frc.robot.commands.CyborgCommandSetTurretPosition;
import frc.robot.commands.CyborgCommandShootPayload;
import frc.robot.commands.CyborgCommandSmartDriveDistance;
import frc.robot.commands.CyborgCommandTestScissorPositition;
import frc.robot.commands.CyborgCommandZeroTurret;
import frc.robot.commands.ManualCommandDrive;
import frc.robot.commands.SemiManualCommandRunWinch;
import frc.robot.commands.ToggleCommandDriveClimber;
import frc.robot.enumeration.AutoMode;
import frc.robot.enumeration.DriveScheme;
import frc.robot.subsystems.SubsystemClimb;
import frc.robot.subsystems.SubsystemDrive;
import frc.robot.subsystems.SubsystemFeeder;
import frc.robot.subsystems.SubsystemFlywheel;
import frc.robot.subsystems.SubsystemIntake;
import frc.robot.subsystems.SubsystemReceiver;
import frc.robot.subsystems.SubsystemSpinner;
import frc.robot.subsystems.SubsystemTurret;
import frc.robot.util.Util;
import frc.robot.util.Xbox;

/**
 * This class is where the bulk of the robot should be declared.  Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls).  Instead, the structure of the robot
 * (including subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  /**
   * Subsystems
   */
  private final SubsystemDrive     SUB_DRIVE    = new SubsystemDrive();
  private final SubsystemTurret    SUB_TURRET   = new SubsystemTurret();
  private final SubsystemIntake    SUB_INTAKE   = new SubsystemIntake();
  private final SubsystemFeeder    SUB_FEEDER   = new SubsystemFeeder();
  private final SubsystemFlywheel  SUB_FLYWHEEL = new SubsystemFlywheel();
  private final SubsystemSpinner   SUB_SPINNER  = new SubsystemSpinner();
  private final SubsystemClimb     SUB_CLIMB    = new SubsystemClimb();
  private final SubsystemReceiver  SUB_RECEIVER = new SubsystemReceiver();
  private final CameraHub          CAMERA_HUB   = new CameraHub();

  /**
   * Controllers
   */
  private final Joystick
    DRIVER   = new Joystick(0),
    OPERATOR = new Joystick(1),
    DRIVER2  = new Joystick(2);

  /**
   * Important Commands
   */
  private final CyborgCommandFlywheelVelocity driveFlywheelRPM = new CyborgCommandFlywheelVelocity(SUB_FLYWHEEL);

  /**
   * Dashboard Items
   */
  private SendableChooser<AutoMode> autoChooser;
  private SendableChooser<DriveScheme> driveChooser;

  /**
   * Auto
   */
  private IAuto currentAuto;
  private Command autoCommand;

  /**
   * The container for the robot.  Contains subsystems, OI devices, and commands.
   */
  public RobotContainer() {
    // Configure the button bindings
    configureButtonBindings();
    configureChoosers();
  }

  /**
   * Schedules the autonomous command.
   */
  public void startAuto() {
    AutoMode desiredAuto = autoChooser.getSelected();

    //create the auto based on the enum
    switch(desiredAuto) {
      case INIT_ONLY:
        currentAuto = new InitAuto(SUB_DRIVE, SUB_TURRET);
        break;
      case THE_BARE_MINIMUM:
        currentAuto = new BareMinimumAuto(SUB_DRIVE, SUB_TURRET, SUB_RECEIVER, SUB_INTAKE, SUB_FEEDER, SUB_FLYWHEEL);
        break;
      case SIX_BALL_SIMPLE:
        currentAuto = new SixBallSimpleAuto(SUB_DRIVE, SUB_TURRET, SUB_RECEIVER, SUB_INTAKE, SUB_FEEDER, SUB_FLYWHEEL);
        break;
      case EIGHT_BALL_TRENCH:
        currentAuto = new TrenchAuto(SUB_DRIVE, SUB_TURRET, SUB_RECEIVER, SUB_INTAKE, SUB_FEEDER, SUB_FLYWHEEL);
        break;
      default:
        currentAuto = new InitAuto(SUB_DRIVE, SUB_TURRET);
        break;
    }

    autoCommand = currentAuto.getCommand();
    autoCommand.schedule();

    //start flywheel if necessary
    if(currentAuto.requiresFlywheel()) {
      if(Constants.AUTO_OVERREV_TURRET) {
        driveFlywheelRPM.overrideRPM(Util.getAndSetDouble("FW Velocity Target", 6000) + (double) Constants.AUTO_OVERREV_EXTRA_RPM);
      }

      driveFlywheelRPM.schedule();
    }
  }
  
  /**
   * Cancels the autonomous command.
   */
  public void cancelAuto() {
    if(currentAuto != null) {
      if(autoCommand.isScheduled()) {
        autoCommand.cancel();
      }
    } else {
      DriverStation.reportError("NO AUTO STARTED, THEREFORE NONE CANCELLED.", false);
    }
  }

  /**
   * Returns the first driver joystick. May be an Xbox controller, but also
   * could be Logitech attack 3. 
   * @return 1st (right) drive joystick
   */
  public Joystick getDriver() {
    return DRIVER;
  }

  /**
   * Returns the operator joystick. Could be Xbox, might be Logitech atk3.
   * @return Operator Joystick.
   */
  public Joystick getOperator() {
    return OPERATOR;
  }

  /**
   * Returns the second Driver joystick, only used for true tank drive.
   * Will NOT always be defined as sometimes it will not be plugged in.
   * @return 2nd (left) drive joystick
   */
  public Joystick getDriver2() {
    return DRIVER2;
  }

  public DriveScheme getDriveScheme() {
    return driveChooser.getSelected();
  }

  /**
   * Prints dashboard indicators indicating whether the robot subsystems are ready for a match.
   * Indicators are to be used for pre-match only. They do not provide an accurite indication
   * of the state of a subsystem in mid match.
   */
  public void printAllSystemsGo() {
    boolean climbIsGo     = SUB_CLIMB.getSystemIsGo();
    boolean driveIsGo     = SUB_DRIVE.getSystemIsGo();
    boolean feederIsGo    = SUB_FEEDER.getSystemIsGo();
    boolean flywheelIsGo  = SUB_FLYWHEEL.getSystemIsGo();
    boolean intakeIsGo    = SUB_INTAKE.getSystemIsGo();
    boolean kiwilightIsGo = SUB_RECEIVER.getSystemIsGo();
    boolean spinnerIsGo   = SUB_SPINNER.getSystemIsGo();
    boolean turretIsGo    = SUB_TURRET.getSystemIsGo();

    boolean allSystemsGo = 
      climbIsGo &&
      driveIsGo &&
      feederIsGo &&
      flywheelIsGo &&
      intakeIsGo &&
      kiwilightIsGo &&
      spinnerIsGo &&
      turretIsGo;
    
    SmartDashboard.putBoolean("All Systems Go", allSystemsGo);
  }

  /**
   * Use this method to define your button->command mappings.  Buttons can be created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a
   * {@link edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {
    /**
     * DRIVER controls
     */
    //manual commands
    SUB_DRIVE.setDefaultCommand(new ManualCommandDrive(SUB_DRIVE));



    SUB_SPINNER.setDefaultCommand(
      new RunCommand(() -> SUB_SPINNER.driveByController(DRIVER), SUB_SPINNER)
    );

    //button commands
    JoystickButton moveClimberUp = new JoystickButton(DRIVER, Xbox.START);
    moveClimberUp.toggleWhenPressed(new ButtonCommandMoveClimber(SUB_CLIMB, 1));

    JoystickButton moveClimberDown = new JoystickButton(DRIVER, Xbox.BACK);
    moveClimberDown.toggleWhenPressed(new ButtonCommandMoveClimber(SUB_CLIMB, -1));

    /**
     * OPERATOR controls
     */
    //manual commands
    SUB_TURRET.setDefaultCommand(
      new RunCommand(() -> SUB_TURRET.moveTurret(OPERATOR), SUB_TURRET)
    );

    SUB_INTAKE.setDefaultCommand(
      new ButtonCommandGroupRunIntakeFeeder(SUB_INTAKE, SUB_FEEDER, SUB_TURRET, OPERATOR)
    );

    //toggle commands
    JoystickButton toggleFlywheel = new JoystickButton(OPERATOR, Xbox.START);
      toggleFlywheel.toggleWhenPressed(driveFlywheelRPM);

    ToggleCommandDriveClimber climberManualDrive = new ToggleCommandDriveClimber(SUB_CLIMB, SUB_TURRET, OPERATOR);
    JoystickButton toggleClimberManual = new JoystickButton(OPERATOR, Xbox.BACK);
      toggleClimberManual.toggleWhenPressed(climberManualDrive);

    //button commands
    CyborgCommandAlignTurret alignTurret = new CyborgCommandAlignTurret(SUB_TURRET, SUB_RECEIVER);
    JoystickButton toggleAlign = new JoystickButton(OPERATOR, Xbox.RB);
      toggleAlign.toggleWhenPressed(alignTurret);

    /**
     * Dashboard Buttons
     */
    SmartDashboard.putData("Drive Flywheel RPM", driveFlywheelRPM);
    SmartDashboard.putData("Align Turret", new CyborgCommandAlignTurret(SUB_TURRET, SUB_RECEIVER, true));
    SmartDashboard.putData("Calibrate Turret Yaw", new CyborgCommandCalibrateTurretYaw(SUB_TURRET));
    SmartDashboard.putData("Calibrate Turret Pitch", new CyborgCommandCalibrateTurretPitch(SUB_TURRET));
    SmartDashboard.putData("Zero Scissor and Winch Encoders", new InstantCommand(() -> SUB_CLIMB.zeroEncoders(), SUB_CLIMB));
    SmartDashboard.putData("Test Scissor PID", new CyborgCommandTestScissorPositition(SUB_CLIMB, OPERATOR));
    SmartDashboard.putData("Zero Turret", new CyborgCommandZeroTurret(SUB_TURRET));
    SmartDashboard.putData("Set Turret Position", new CyborgCommandSetTurretPosition(SUB_TURRET, 500000, 0));
    SmartDashboard.putData("Drive Distance", new CyborgCommandDriveDistance(SUB_DRIVE, -132, 0.75));
    SmartDashboard.putData("Zero Yaw", new InstantCommand(() -> SUB_TURRET.setCurrentYawEncoderPosition(0), SUB_TURRET));
    SmartDashboard.putData("Zero Drivetrain Encoders", new InstantCommand(() -> SUB_DRIVE.zeroEncoders()));
    SmartDashboard.putData("Apply Camera Settings", new InstantCommand(() -> CAMERA_HUB.configureCameras()));
    SmartDashboard.putData("Drive Just Masters", new RunCommand(() -> SUB_DRIVE.driveJustMasters(DRIVER), SUB_DRIVE));
    SmartDashboard.putData("Drive Just Slaves", new RunCommand(() -> SUB_DRIVE.driveJustSlaves(DRIVER), SUB_DRIVE));
    SmartDashboard.putData("Drive Straight", new CyborgCommandSmartDriveDistance(SUB_DRIVE, 60, 0.6));
    SmartDashboard.putData("Shoot Payload", new CyborgCommandShootPayload(SUB_INTAKE, SUB_FEEDER, SUB_FLYWHEEL, SUB_RECEIVER, SUB_TURRET, 3, 15000, false));

    SmartDashboard.putData("Toggle Winch", climberManualDrive);
    SmartDashboard.putData("Drive Flywheel RPM", driveFlywheelRPM);
  }
  
  private void configureChoosers() {
    //declare the different autos we will choose from
    autoChooser = new SendableChooser<AutoMode>();
    autoChooser.setDefaultOption("Bare Minimum Auto", AutoMode.THE_BARE_MINIMUM);
    autoChooser.addOption("Init Only", AutoMode.INIT_ONLY);
    autoChooser.addOption("Simple Six Ball", AutoMode.SIX_BALL_SIMPLE);
    autoChooser.addOption("Eight Ball", AutoMode.EIGHT_BALL_TRENCH);
    SmartDashboard.putData("Auto Mode", autoChooser);

    driveChooser = new SendableChooser<DriveScheme>();
    driveChooser.setDefaultOption("Rocket League", DriveScheme.RL);
    driveChooser.addOption("True Tank", DriveScheme.TRUE_TANK);
    SmartDashboard.putData("Drive Scheme", driveChooser);
  }
}
