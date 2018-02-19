package org.usfirst.frc.team2228.robot;

import org.usfirst.frc.team2228.commands.EncoderTurn;
import org.usfirst.frc.team2228.commands.MoveTo;
import org.usfirst.frc.team2228.commands.RotateTo;
import org.usfirst.frc.team2228.commands.StringCommand;
import org.usfirst.frc.team2228.commands.Switch;

import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.CommandGroup;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class AutoMaster {
	final String defaultAuto = "Baseline";
	final String customAuto = "Switch";
	private char[] positions;
	private SRXDriveBase base;
	private String autoSelected;
	private String input = "";
	private CommandGroup Cg;
	private SendableChooser<String> chooser = new SendableChooser<>();
	private String robotSide = "Right";
	private Elevator elevator;
	
	public AutoMaster(SRXDriveBase srxdb, Elevator _elevator) {
		base = srxdb;
		chooser.addDefault("Switch", customAuto);
		elevator = _elevator;
		chooser.addObject("Baseline", defaultAuto);
		
		SmartDashboard.putData("Auto choices", chooser);
		//BOOP BEEP BOP BEEPEDIE BOOP BOP 
		

	}
	public void init() {
		base.setRightEncPositionToZero();
		base.setLeftEncPositionToZero();
		
		Cg = new CommandGroup();
		
		
		autoSelected = chooser.getSelected();
		String gameData;
		gameData = DriverStation.getInstance().getGameSpecificMessage();
		positions = gameData.toCharArray();
		System.out.println("Auto selected: " + autoSelected);
		if(gameData.charAt(0) == 'L'){
			System.out.println("L");
		}else{
			System.out.println("R");
	}
		switch (autoSelected) {
			case "Baseline":
				System.out.println("Baseline selected");
				Cg.addSequential(new MoveTo(base, (Dimensions.AUTOLINE_TO_ALLIANCE - 
						                           Dimensions.LENGTH_OF_ROBOT), 0.2, false));
				break;
				
			case "Switch":
				System.out.println("Switch selected");
				Cg.addSequential(new MoveTo(base, (Dimensions.SWITCHWALL_TO_ALLIANCESTATION - 
						                           Dimensions.LENGTH_OF_ROBOT), 0.2, false));
				//if ((robotSide == "Left" && L) || (robotSide == "Right" && R)){
				Cg.addSequential(new Switch(elevator));
					
				// Scale cube command
				break;
		}
		Cg.start();
	}
	public void run() {
		Scheduler.getInstance().run();
	}
}
