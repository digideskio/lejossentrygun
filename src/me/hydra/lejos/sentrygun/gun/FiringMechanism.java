package me.hydra.lejos.sentrygun.gun;

import lejos.nxt.LCD;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.Sound;

/**
 * Threaded firing mechanism for a gun.
 * 
 * The sentry gun has a motor which controls two pistons, for each turn of the motor two balls are fired as the pistons are out of phase.
 * 
 * The gun tells the firing mechanism to fire one (or more) rounds and can continue doing so even while the mechanism is firing.
 *
 * @todo very occasionally the gun jams if the piston is not in the correct place with the program starts
 * since there's no way to detect the position of the pistons (without accidentally firing rounds) we need to be able to detect
 * when the firing motor stalls and react appropriately
 * 
 * @todo provide a way to abort the firing sequence after the current round has been fired
 * 
 * @author Hydra <contact@hydra.me>
 */
public class FiringMechanism extends Thread {

	protected NXTRegulatedMotor motor;
	protected int queuedRounds = 0;
	protected int newQueuedRounds = 0;
	protected boolean firing = false;
	boolean die = false;

	public FiringMechanism(MotorPort port) {	
		motor = new NXTRegulatedMotor(port);
	}

	public synchronized int queuedRounds() {
		return queuedRounds;
	}
	
	public synchronized void fireRounds(int roundsToFire) {
		if (roundsToFire < 0) {
			throw new RuntimeException("Negative value used");
		}
		
		if (!isFiring()) {
			queuedRounds += roundsToFire;
			this.interrupt();
		} else {
			synchronized (this) {
				newQueuedRounds += roundsToFire;
			}
		}
	}
	
	public synchronized void die() {
		die = true;
		this.interrupt();
	}
	
	public synchronized boolean isFiring() {
		return firing;
	}
	
	public void run() {		
		
		int sequenceIndex = 0;
		String sequence = "-\\|/"; 
		while (!die) {			
			LCD.drawString(sequence.charAt(sequenceIndex++) + "", 0, 3);
			if (die) {
				Sound.playTone(2349, 115);
				Sound.playTone(1760, 115);
				LCD.drawString("DIE!", 2, 3);
			}
			if (sequenceIndex == sequence.length()) {
				sequenceIndex = 0;
			}
			
			boolean doWaitAndContinue = false;
			
			synchronized(this) {
				if (this.queuedRounds == 0) {
					doWaitAndContinue = true;
				}
			}

			if (doWaitAndContinue) {
				try { 
					//Sound.playTone(3000, 20);
					Thread.sleep(250);
				} catch (InterruptedException e) {
					// Wake-up, sleepy!
				}
				
				continue;
			}

			synchronized(this) {
				firing = true;
			}
			LCD.drawString("FIRING!", 2, 3);

			motor.resetTachoCount();
			int rotatedSoFar = motor.getTachoCount(); // should be 0 if the motor wasn't rotating
			
			final int degreesToFireOneRound = 180;
			int degreesToRotateTo = 0;
			int firedRounds = 0;
			int initialQueuedRounds = queuedRounds;
			
			boolean done = false;
			do {
				synchronized(this) {					
					rotatedSoFar = motor.getTachoCount();						
					firedRounds = (int)java.lang.Math.floor(rotatedSoFar / degreesToFireOneRound);
					queuedRounds = initialQueuedRounds - firedRounds;
					degreesToRotateTo = initialQueuedRounds * degreesToFireOneRound;
					
					motor.rotateTo(degreesToRotateTo, true);
					
					if (queuedRounds == 0) {
						done = true;
					}
					
					if (newQueuedRounds > 0) {						
						initialQueuedRounds += newQueuedRounds;
						newQueuedRounds = 0;
					}
				}

				
				synchronized(this) {
					if (die) {
						done = true;
					}
				}

				LCD.drawString("To: " + degreesToRotateTo + " ", 0, 4);
				LCD.drawString("Turned: " + rotatedSoFar + " ", 0, 5);
				LCD.drawString("Fired: " + firedRounds + " ", 0, 6);
				LCD.drawString("To Fire: " + queuedRounds + " ", 0, 7);
				
				if (!done) {
					// wait a while
					try { 
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
			} while (!done);

			// reset motor position
			int resetAngle = rotatedSoFar % degreesToFireOneRound;
			LCD.drawString("Out By: " + resetAngle + " ", 0, 5);
			motor.rotate(0-resetAngle);

			synchronized(this) {
				firing = false;
			}
			LCD.drawString("READY  ", 2, 3);
		};
		
		
	}
}
