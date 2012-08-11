package me.hydra.lejos.sentrygun.tilter;

import lejos.nxt.LCD;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.addon.AngleSensor;

/**
 * todo take more readings from a TiltSensor during calibration
 * todo add methods that allow the Tilter to be moved to a given angle (use the readings from the Tilt sensor 

 * @author Hydra <contact@hydra.me>
 */
public class Tilter {

	protected NXTRegulatedMotor motor; // to tilt, forwards = tilt down, backwards = tilt up
	protected AngleSensor angleSensor;
	
	protected int rackEndToEndDegrees;
	protected int tiltReadingMin;
	protected int tiltReadingMax;
	
	public Tilter(MotorPort motorPort, SensorPort sensorPort) {
	
		motor = new NXTRegulatedMotor(motorPort);
		
		angleSensor = new AngleSensor(sensorPort);
		
		calibrate();
	}

	public void calibrate() {
		determineRackLength();
		tiltDown();
		
		
	}
	
	/**
	 * tilt downwards as far as possible
	 */
	public void tiltUp() {
		motor.rotateTo(rackEndToEndDegrees);
	}

	/**
	 * tilt upwards as far as possible
	 */
	public void tiltDown() {
		motor.rotateTo(0);
	}

	/**
	 * determine the rack length
	 * 
	 * this is achieved by the following sequence:
	 * 
	 * move the tilter downwards until it stops
	 * reset the tacho
	 * move the tilter upwards until it stops
	 * take the tacho reading
	 * 
	 * once the above sequence is complete it's possible to know the length (in degrees of rotation) of the rack
	 * this value is stored in rackEndToEndDegrees
	 * 
	 * if a high power is user the pinion gears slip on the rack, a lower power must be used so that the motor can stall
	 * so that the stall can be detected 
	 *  
	 */
	protected void determineRackLength() {
		LCD.drawString("Tilter Init ", 0, 0);
		
		//
		// move up
		//
		
		motor.backward();
		
		while (!motor.isMoving()) {		
			// wait for motor to begin rotating
			try {Thread.sleep(150); } catch (Exception e) {}
		}
		do {
			// wait for motor to stop
			try {Thread.sleep(150); } catch (Exception e) {}
		} while (motor.isStalled());
		motor.stop();
		
		// wait a moment
		
		try {Thread.sleep(250); } catch (Exception e) {}
			
		// take a tilt reading
		tiltReadingMin = angleSensor.getAngle();
		
		// reset so we know 0 - downmost tilt position
		motor.resetTachoCount();

		//
		// move down
		//
		
		motor.forward();
		
		while (motor.getSpeed() == 0) {		
			// wait for motor to begin rotating
			try {Thread.sleep(150); } catch (Exception e) {}
		}
		do {			
			// wait for motor to stop
			try {Thread.sleep(150); } catch (Exception e) {}
		} while (motor.getSpeed() != 0);
		motor.stop();

		// take another reading		
		rackEndToEndDegrees = motor.getTachoCount();

		// now rackEndToEndDegrees = downmost, 0 = upmost
		
		// take tilt reading
		tiltReadingMax = angleSensor.getAngle();
	}
}
