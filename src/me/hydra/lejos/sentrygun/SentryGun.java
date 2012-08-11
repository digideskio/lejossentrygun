package me.hydra.lejos.sentrygun;

import lejos.nxt.*;
import me.hydra.lejos.sentrygun.gun.Gun;
import me.hydra.lejos.sentrygun.tilter.Tilter;

public class SentryGun {

	protected static final int Clockwise = 1;
	protected static final int AntiClockwise = 2;

	protected static final int magazineSize = 20;	
	protected static Gun gun;
	
	protected static Tilter tilter;
	
	protected static UltrasonicSensor proximitySensor;
	protected static NXTRegulatedMotor angle;
	/**
	 * add some listeners to the buttons:
	 * 
	 * pressing ENTER queues a round to be fired (so long as there's sufficient ammo)
	 * pressing RIGHT fires 5 rounds (so long as there's sufficient ammo)
	 * pressing LEFT notifies the gun that it has been reloaded (as long as it's not firing)
	 */
	
	protected static void addButtonListeners() {
		Button.ENTER.addButtonListener(new ButtonListener() {
			boolean doQueueRound; 
			public void buttonPressed(Button b) {
				int unqueuedRounds = gun.getUnqueuedRounds();
				if (unqueuedRounds > 0) {
					doQueueRound = true;
					LCD.drawString("Queuing round", 0, 1);
				} else {
					doQueueRound = false;
					LCD.drawString("No Rounds    ", 0, 1);
				}
		    }

		    public void buttonReleased(Button b) {
		    	LCD.drawString("             ", 0, 1);
		    	if (doQueueRound) {
		    		gun.fireSingleRound();
		    	}
		    }
		});

		Button.RIGHT.addButtonListener(new ButtonListener() {
			boolean doQueueRound; 
			public void buttonPressed(Button b) {
				int unqueuedRounds = gun.getUnqueuedRounds();
				if (unqueuedRounds >= 5) {
					doQueueRound = true;
					LCD.drawString("Queuing 5 rounds", 0, 1);
				} else {
					doQueueRound = false;
					LCD.drawString("Insufficient rds", 0, 1);
				}
		    }

		    public void buttonReleased(Button b) {
		    	LCD.drawString("                ", 0, 1);
		    	if (doQueueRound) {
		    		gun.fireRounds(5);
		    	}
		    }
		});
		
		Button.LEFT.addButtonListener(new ButtonListener() {
			boolean doReload;
			public void buttonPressed(Button b) {				
				if (!gun.isFiring()) {
					LCD.drawString("Reloading   ", 0, 1);
					doReload = true;
				} else {
					LCD.drawString("Still firing", 0, 1);
					doReload = false;
				}
		    }

		    public void buttonReleased(Button b) {
		    	LCD.drawString("             ", 0, 1);
		    	if (doReload) {
		    		gun.reload();
		    	}
		    }
		});
	}

	public static void ensureEnterReleased() {
		while (Button.ENTER.isDown()) {
			LCD.drawString("Release ENTER", 0, 0);
			try { Thread.sleep(50); } catch (Exception e) {}
		}
		LCD.clear();
	}
	
	public static void initialise() {
		LCD.clear();
		ensureEnterReleased();
		
		/*
		 * create a new Tilter instance (the creation of which starts a calibration sequence, etc)
		 */
		tilter = new Tilter(MotorPort.B, SensorPort.S2);
		
		gun = new Gun(MotorPort.A);
		gun.setMagazineSize(magazineSize);

		proximitySensor = new UltrasonicSensor(SensorPort.S1);
		
		angle = new NXTRegulatedMotor(MotorPort.C);
		angle.resetTachoCount();
		
		addButtonListeners();
	}
	
	public static void main(String[] args) throws Exception {

		initialise();

		scan();

		shutdown();
	}
	
	public static void shutdown() {
		LCD.clear();
		
		// start rotating back to 0 then stop the gun while we're waiting for the rotate
		angle.rotateTo(0, true);
		LCD.drawString("Gun >> STOP    ", 0, 0);
		try { gun.shutdown(); } catch (InterruptedException e) {}
		LCD.drawString("Angle >> 0     ", 0, 0);
		while (angle.isMoving()) {
			try { Thread.sleep(50); } catch (Exception e) {}
		}
		LCD.clear();
		
		LCD.drawString("System Inactive", 0, 0);
	}
	
	
	
	public static void scan() {
		
		int tickEvery = 50;
		int tick = 0;
		
		int direction = Clockwise;
		do {
			//
			// make a ticking noise
			//
			
			if (tick % tickEvery == 0) {
				Sound.setVolume(4);
				Sound.playTone(2000, 20);
				tick = 0;
			}
			tick++;
			
			try { Thread.sleep(50); } catch (Exception e) {}
			
			//
			// rotate the gun left and right unless the gun is firing (object detected) or the magazine is empty (waiting for reload)
			//
			if (!gun.isFiring() && !gun.isEmpty()) {

				//
				// check the position of the gun, if it's close enough to either limit
				// then reverse the direction
				//
				
				int currentAngle = Math.abs(angle.getTachoCount());
				if (currentAngle > 715 && currentAngle < 725) { 
					direction = (direction == Clockwise ? AntiClockwise : Clockwise); 
				}
				
				if (direction == Clockwise) {
					angle.rotateTo(720, true);
				} else {
					angle.rotateTo(-720, true);
				}
			}
			
			int distance = proximitySensor.getDistance();
			int roundsRemaining = gun.getRemainingRounds();
			int unqueuedRounds = gun.getUnqueuedRounds();

			//
			// status display
			//
			LCD.drawString(unqueuedRounds + "/" + roundsRemaining + "/" + magazineSize + " " + distance + "cm ", 0, 0);
			
			//
			// check for object to fire at
			//
			if (distance < 150) {
				
				//
				// hold position and shoot
				//
				angle.stop();
				
				Sound.setVolume(5);
				gun.fireSingleRound();
				Sound.beepSequenceUp();
			}
			
			//
			// check for shutdown request
			//
			if (Button.ESCAPE.isDown()) {
				LCD.drawString("Release ESCAPE", 0, 0);
				break;
			}
		} while(true);
				
		while (Button.ESCAPE.isDown()) {
			try { Thread.sleep(50); } catch (Exception e) {}
		}
	}

}
