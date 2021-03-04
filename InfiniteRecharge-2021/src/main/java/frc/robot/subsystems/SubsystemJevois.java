// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.hal.util.UncleanStatusException;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.SerialPort;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

public class SubsystemJevois extends SubsystemBase {
  SerialPort port;
  private int
    closestBallX,
    closestBallY,
    closestBallRadius;

  private String 
    currentMessage,
    lastCompletedMessage;

  private boolean portInitalized;
  private long lastUpdatedTime;

  /** Creates a new SubsystemJevois. */
  public SubsystemJevois() {
    closestBallX = -1;
    closestBallY = -1;
    closestBallRadius = -1;
    currentMessage = "";
    lastCompletedMessage = "No Message!";
    portInitalized = false;
    lastUpdatedTime = 0;

    try {
      port = new SerialPort(Constants.JEVOIS_BAUD_RATE, Constants.JEVOIS_PORT);
      portInitalized = true;
    } catch(UncleanStatusException ex) {
      DriverStation.reportError("Unclean Status! Is the right Port specified?", true);
      DriverStation.reportError(ex.getMessage(), false);
    }
  }

  //[x,y,r],[x,y,r]
  @Override
  public void periodic() {
    if(portInitalized) {
      currentMessage += port.readString();
    } else {
      DriverStation.reportError("SubsystemJevois' port was not initalized!", true);
    }

    int lastNewline = currentMessage.lastIndexOf("\n");
    int secondLastNewline = currentMessage.lastIndexOf("\n", lastNewline - 1);
    if(lastNewline >= 0 && secondLastNewline >= 0) {
      lastCompletedMessage = currentMessage.substring(secondLastNewline + 1, lastNewline);
      currentMessage = currentMessage.substring(lastNewline);
      
      if(!lastCompletedMessage.contains("None")) {
        lastUpdatedTime = System.currentTimeMillis();

        // //process the string and grab the x coordinate, y coordinate, and radius
        // int lastOpenBracket = lastCompletedMessage.lastIndexOf("[");
        // int firstCloseBracket = lastCompletedMessage.indexOf("]");

        // String contentsOfMessage = lastCompletedMessage.substring(lastOpenBracket + 1, firstCloseBracket);
        // contentsOfMessage = reduceSpaces(contentsOfMessage);
        // String[] segments = contentsOfMessage.split(" ");
        // closest
      }
    }

    SmartDashboard.putString("Jevois data", lastCompletedMessage);    
  }

  private String reduceSpaces(String str) {
    boolean spaceUsed = false;
    String newString = "";
    for(int i=0; i<str.length(); i++) {
      char character = str.charAt(i);
      if(character != ' ') {
        newString += character;
        spaceUsed = false;
      } else {
        if(!spaceUsed) {
          newString += character;
          spaceUsed = true;
        }
      }
    }

    return newString;
  }


}
