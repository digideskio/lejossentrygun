package me.hydra.lejos.sentrygun.gun;

import lejos.nxt.MotorPort;
import static java.lang.Math.*;

// forwards = correct rotation for firing mechanism, backwards = do not use (piston action not as powerful due to physical offset)

/**
 * A gun class
 * 
 * The gun knows about it's magazine (clip) and it's firing mechanism.
 * 
 * @author Hydra <contact@hydra.me> 
 */
public class Gun {

    protected MotorPort port = MotorPort.A;
    int unqueuedRounds = 0;
    int magazineSize = 0;
    FiringMechanism firingMechanism;

	public Gun() {
		initialise();
	}
	
	public Gun(MotorPort port) {
		this.port = port;
		initialise();
	}
	
	protected void initialise() {
		firingMechanism = new FiringMechanism(port);
		firingMechanism.start();
	}

	/**
	 * 
	 * @throws InterruptedException
	 */
	public void shutdown() throws InterruptedException {
		firingMechanism.die();
		firingMechanism.join();		
	}
	
	/**
	 * set the magazine (clip) size
	 * 
	 * the gun can only fire as many rounds as the magazine holds
	 */
	public void setMagazineSize(int magazineSize) {
		int difference = this.magazineSize - magazineSize;
		unqueuedRounds = max(0, unqueuedRounds - difference);
		this.magazineSize = magazineSize;		 
	}
	
	/**
	 * returns the live value of the rounds remaining in the magazine
	 * useful for live status of the magazine
	 * @see getUnqueuedRounds
	 */
	public int getRemainingRounds() {
		return unqueuedRounds + firingMechanism.queuedRounds();
	}

	/**
	 * get the amount of rounds available to be fired
	 */
	public int getUnqueuedRounds() {
		return unqueuedRounds;
	}
	
	public boolean isEmpty() {
		return getRemainingRounds() == 0;
	}
	
	/**
	 * instructs the gun to fire a round, returning immediately
	 * 
	 * @return true if the gun has rounds left to fire
	 */
	public boolean fireSingleRound() {
		if (!hasRounds(1)) {
			return false;
		}
		queueRounds(1);
		return true;
	}

	/**
	 * instructs the gun to fire one or more rounds, returns immediately
	 * 
	 * @return the amount of rounds queued (as they may not have been enough rounds left)
	 */
	public int fireRounds(int desiredRoundsToFire) {
		int roundsToFire = min(unqueuedRounds, desiredRoundsToFire);
		if (roundsToFire == 0) {
			return 0;
		}
		queueRounds(roundsToFire);
		return roundsToFire;
	}
	
	/**
	 * check to see if there's enough rounds for the given value
	 * 
	 * @param roundsToFire the amount of rounds to check for
	 * @return true if there is enough rounds
	 */
	public boolean hasRounds(int roundsToFire) {
		return roundsToFire <= unqueuedRounds;
	}
	
	/**
	 * Tell the gun that it's been physically reloaded
	 * 
	 * You can't reload the gun if it's firing (that'd be dangerous!)
	 * 
	 * @see isFiring
	 */
	public void reload() {	
		if (firingMechanism.isFiring()) {
			throw new RuntimeException("Can't reload while firing"); // check first!
		}
		unqueuedRounds = magazineSize;
	}
	
	/**
	 * @see fireSingleRound
	 * @see fireRounds
	 */
	protected void queueRounds(int roundsToFire) {
		if (unqueuedRounds - roundsToFire < 0) {
			throw new RuntimeException("Insufficent rounds"); // check first!
		}
		unqueuedRounds -= roundsToFire;
		firingMechanism.fireRounds(roundsToFire);
	}
	
	public boolean isFiring() {
		return firingMechanism.isFiring();
	}
	
}
