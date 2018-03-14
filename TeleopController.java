package org.usfirst.frc.team2228.robot;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
// 
public class TeleopController {
	
	//================================
	// OBJECTS
	
	private DriverIF DriverIF;
	private SRXDriveBase driveBase;
	
	//================================
	// SWITCHES
	
	public boolean isTeleopConsoleDataEnabled = false;
	public boolean isJoyStick2SRXDriveBaseDisabled = false;
	
	private boolean isStopCheckToggleState = false;
	private boolean isToggleFlagState = false;
	
	//=================================
	// SET POINTS
	
	//  defines minimum joystick moves to be acted on 
    private static double kJoyStickDeadBand = 0.13;
	
	// used to vary the affect when using cubed sensitivity
	// 0 to 1; 0 is linear (output==input) 1 is cubed (output=input**3)
    private static double kThrottleCubedGain = 1.0; 
	
    // 0.1 to 1.0 used for turn sine control
    private static double kTurnSensitivityGain = 0.3;
	
	// Range of smoothFactorValue is .5 to .9999; (no smoothing-0), (high smoothing-.99999)
	private static double kLowSmoothFactor = 0.5;
	private static double kHighSmoothFactor = 0.95;
	//private static double kTransitionSmoothFactor = 0.7;
	
	// determination of max throttle delta values are determined by testing
	private static double kMaxDeltaVelocity = 0.001;
	
	private static double kStopBrakeValue = .1;
	
	//======================================
	// VARIABLES
	private double origThrottle = 0;
	private double origTurn = 0;

	private double turn = 0;
	private double throttle = 0;
	
	private double accelFltrThrottleValue = 0;
	private double deltaAccelFltrThrottleValue = 0;
	private double smoothFactorValue = 0;
	private double EMAThrottleValue = 0;
	private double previousEMAThottleValue = 0;

	private double deltaThrottleForStopValue =0;
	private double previousThrottleForStopValue =0;

	private double maxThrottleLimit = .7;
	private double maxTurnLimit = .3;
	
	private String lastMsgString = " ";
	
    //=========================
	// ENUMs
	
     private enum TurnSensitivity {
		Linear,
		Sine,
		Squared,
	}
    private enum ThrottleSensitivity {
		Linear,
		SCurve,
		Squared,
		Cubed
	}
	private static TurnSensitivity turnSensitivitySet = TurnSensitivity.Squared;
	private static ThrottleSensitivity throttleSensitivitySet = ThrottleSensitivity.Squared;
	
	//==============================================
	// TELEOPCONTROLLER CONSTRUCTOR
	public TeleopController(DriverIF _driverIF, SRXDriveBase _driveBase) {
		DriverIF = _driverIF;
		driveBase = _driveBase;
	}
	
	//==========================================
	// TELEOP INIT
	public void teleopInit(){
		previousEMAThottleValue = 0;
		previousThrottleForStopValue = 0;
		loadSmartDashBoardParmeters(); 
	}
		
	//==========================================
	// TELEOP PERIODIC
	public void teleopPeriodic() {
		
		// Flag for console display
		isTeleopConsoleDataEnabled = SmartDashboard.getBoolean("TstBtn-EnableTeleopConsoleDisplay:", isTeleopConsoleDataEnabled);
		// Flag for disable joysticks to drive base for testing joysticks and accel filter
		isJoyStick2SRXDriveBaseDisabled = SmartDashboard.getBoolean("TstBtn-DisableJoy2SRXDrvBase:", isJoyStick2SRXDriveBaseDisabled);
		
		// Save the joystick values
		origThrottle = -DriverIF.Throttle();
		origTurn = DriverIF.Turn();
		throttle = origThrottle;
		turn = origTurn;

		throttle = normalize(throttle);
		turn = normalize(turn);
		
		// modify throttle/turn to sensitivity curves
		if(turn != 0){
			turn = CheckTurnSensitivityFilter(throttle, turn);
		}
		if(throttle != 0){
			throttle = CheckThrottleSensitivity(throttle);
			throttle = CheckAccelFilter(throttle);
		//	throttle = CheckDriverStopping(throttle);
		}
		
		// Re-check that values are not over 1
		throttle = cap(throttle);
		turn = cap(turn);
		
		// ============================================
		// Limit max throttle / turn
		throttle *= maxThrottleLimit;
		turn *= maxTurnLimit;
		
		// Apply dead band on joysticks
		throttle = joyDeadBand(throttle);
		turn = joyDeadBand(turn);
		
		// =======================================
		// DRIVE ROBOT
		if(!isJoyStick2SRXDriveBaseDisabled){
			driveBase.setThrottleTurn(throttle, turn);
		}
		
		// ++++++++++++++++++++++++++++++++++++
		// Display
		if (isTeleopConsoleDataEnabled){
			System.out.printf("Thottle:%-4.2f==SmFac:%-4.2f ==accelthrot:%-4.2f ==Delta: %-4.2f ==EMA:%-4.2f%n", 
								throttle,
								smoothFactorValue,
								accelFltrThrottleValue,
								deltaAccelFltrThrottleValue,
								EMAThrottleValue);
		}
	}	
								
	
	//=======================================
	// SET METHODS
	public void SetMaxThrottlePower(double _maxThrottleLimitPowerLevel) {
	maxThrottleLimit = _maxThrottleLimitPowerLevel;
	}
	
	public void SetMaxTurnPower(double _maxTurnLimitPowerLevel) {
	maxTurnLimit = _maxTurnLimitPowerLevel;
	}
	// ========================================
	// SHUFFLEBOARD
	private void loadSmartDashBoardParmeters() {
		SmartDashboard.putBoolean("TstBtn-EnableTeleopConsoleDisplay:", isTeleopConsoleDataEnabled);
		SmartDashboard.putBoolean("TstBtn-DisableJoy2SRXDrvBase:", isJoyStick2SRXDriveBaseDisabled);
	}
	// ===========================================
	// DRIVERIF FILTERING FUNCTIONS

	public double CheckTurnSensitivityFilter(double _throttle, double _turn) {
		
		double fThrottle = _throttle;
		double fTurn = _turn;
		
		switch (turnSensitivitySet) {
		case Linear:
			// no change
			break;
			
		case Sine:
			//developed by team 254 for the turn stick to provide a more realistic feel for turning???
			fTurn = ApplySineFunction(fTurn);
			fTurn = ApplySineFunction(fTurn);
			fTurn = ApplySineFunction(fTurn);
			break;
			
		case Squared:
			fTurn = fTurn * Math.abs(fTurn);
			break;
		
		default:
			break;
		}
		return fTurn;
	}
	
	public double ApplySineFunction(double _turn) {
		// kTurnSensitivityGain should be 0.1 to 1.0 used for chezy turn
		// control
		double factor = (Math.PI / 2.0) * kTurnSensitivityGain;
		return Math.sin(factor * _turn) / Math.sin(factor);
	}
	
	public double CheckThrottleSensitivity(double _throttle) {

		// Sensitivity modifies the input value to provide a different feel of robot motion to the operator. 
		// There are several sensitivity curves that adjust the operator input for driving the robot. 
		// - Linear sensitivity curve: 	The action is linear for the operator, output == input
		// - Sine wave sensitivity: 	The sensitivity provides a larger do little around zero speed and near full speed. 
		//                          	This is the typical elevator curve. 
		// - Squared sensitivity curve: This sensitivity slows down the response of the robot to fast moves on the part of the operator. 
		// - Cubed sensitivity curve: 	This sensitivity really slows down the response of the robot.
		
		double fThrottle = _throttle;

		switch (throttleSensitivitySet) {
		case Linear:
			// no change
			break;
		case SCurve:
			fThrottle = SCurve(_throttle);
			break;
		case Squared:
			fThrottle = _throttle * Math.abs(_throttle);
			break;
		case Cubed:
			// kThrottleCubedGain => from 0 to 1; 0 = linear, 1 = cubed
			fThrottle = (kThrottleCubedGain * (Math.pow(_throttle, 3)))	+ ((1 - kThrottleCubedGain) * _throttle);
			break;
		default:
			break;
		}
		return fThrottle;
	}
	
	public double SCurve(double _value) {
		double adjustedValue = _value;
		if (_value < 0) {
			adjustedValue = (2 * (-(Math.pow(_value, 3)))) - (3 * (Math.pow(_value, 2)));
		} else if (_value > 0) {
			adjustedValue = (3 * (Math.pow(_value, 2))) - (2 * (-(Math.pow(_value, 3))));
		}
		return adjustedValue;
	}
	// ANTI-LOCK BRAKING IN TELEOP
	public double CheckDriverStopping(double _throttleForStopValue){
		// check every two scans
		isStopCheckToggleState = !isStopCheckToggleState;
		if(isStopCheckToggleState){
			deltaThrottleForStopValue = _throttleForStopValue - previousThrottleForStopValue;
			previousThrottleForStopValue = _throttleForStopValue;
			// check if decelerating
			if(((_throttleForStopValue > 0) && (deltaThrottleForStopValue < 0 )) || ((_throttleForStopValue < 0) && (deltaThrottleForStopValue > 0 ))){
				
				// Decelerating and within braking band apply reverse current to motors
				if((Math.abs(_throttleForStopValue) < 0.5) &&  (Math.abs(_throttleForStopValue) > 0.1)){
					_throttleForStopValue = - Math.signum(_throttleForStopValue)* kStopBrakeValue;
				}
			} 
		}
		return _throttleForStopValue;
	}
	

	// ACCELERATION FILTER
	// The accel filter follows the actions of the driver. If the
	// driver exceeds the cap of robot accel/decel capability the tipping
	// filter slows the response of the throttle to protect the robot.
	// Tipping filter addresses:
	// 1 - wheelies
	// 2 - fast accels
	// 3 - transitions from fwd/rev or rev/fwd
	public double CheckAccelFilter(double _ThrottleValue) {
		
		// makes execution 2xscan = approx 40ms now
		isToggleFlagState = !isToggleFlagState;
		
		if(isToggleFlagState){
			accelFltrThrottleValue = _ThrottleValue;
		
			// determine change for last driverIF read
			deltaAccelFltrThrottleValue = accelFltrThrottleValue - previousEMAThottleValue;

			// Check for large same sign delta value that may cause a wheelie or rotation torque to a high Center of gravity
			if (Math.abs(deltaAccelFltrThrottleValue) > kMaxDeltaVelocity) {
					smoothFactorValue = kHighSmoothFactor;
				} else {
					// If driver behaves
					smoothFactorValue = kLowSmoothFactor;
				}
			
			// RUN THROUGH SMOOTHING FILTER
			// Exponential Avg Filter (EMA) is a recursive low pass filter that can change it's gain to address filter response
			// newAverage = alpha*presentValue + (1-alpha)*lastValue or:
			EMAThrottleValue = previousEMAThottleValue + (1-smoothFactorValue) * (deltaAccelFltrThrottleValue);
			previousEMAThottleValue = EMAThrottleValue;
		}
		return  EMAThrottleValue;
	}
	
	// ============================================
	// UTIL METHODS
	
	//helper function to keep inside of acceptable %power range
	private double cap(double num) {
		if(Math.abs(num) > 1){
			num = Math.signum(num)* 1.0;
		} 
		return num;
	}
	// This caps number and remaps to 0 -> 1 range
	private double normalize(double num) {
		if(Math.abs(num) > 1){
			num = Math.signum(num)* 1.0;
		} else if((Math.abs(num) >= kJoyStickDeadBand) ){
			// This normalizes data from (kJoyStickDeadBand -> 1) to (0 -> 1)
			if(num >= 0){
				num = (num - kJoyStickDeadBand) / (1 - kJoyStickDeadBand);
			} else {
				num = (num + kJoyStickDeadBand) / (1 - kJoyStickDeadBand);
			}	
		}
		return num;
	}
	
	private double joyDeadBand(double num){
		if (Math.abs(num) <= kJoyStickDeadBand) {
			num = 0;
		}
		return num;
	}
	
	private void msg(String _msgString){
		if (isTeleopConsoleDataEnabled){
			if (_msgString != lastMsgString){
				System.out.println(_msgString);
				lastMsgString = _msgString;
			}
		}
	}
}