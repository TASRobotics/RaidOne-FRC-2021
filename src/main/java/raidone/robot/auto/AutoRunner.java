package raidone.robot.auto;

import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import raidone.robot.auto.sequences.*;
import raidone.robot.auto.sequences.SlalomPathSequence;
import raidone.robot.dashboard.Tab;
import raidone.robot.submodules.Drive;

/**
 * Class that manages autonomous sequences.
 */
public class AutoRunner {

    private SendableChooser<AutoSequence> chooser;

    private AutoSequence selectedSequence;
    private static Drive drive = Drive.getInstance();

    private AutoSequence[] availableSequences = { //list out sequences
        new TestSequence(),
        //new StealCellSequence(),
        new SlalomPathSequence(),
        new BouncePathSequence(),
        new BarrelPathSequence()
    };

    public AutoRunner() {
        chooser = new SendableChooser<>();
        chooser.setDefaultOption("None", new EmptySequence());
        for (AutoSequence sequence : availableSequences) {
            chooser.addOption(sequence.getName(), sequence);
        }
        Shuffleboard.getTab(Tab.MAIN)
                .add("Auton Selection", chooser)
                .withSize(3, 1)
                .withPosition(2, 1);
        // SmartDashboard.putData("Auton Selection", chooser);
    }

    /**
     * Reads the selected autonomous sequence from the SendableChooser.
     */
    public void readSendableSequence() {
        selectSequence(chooser.getSelected());
    }

    /**
     * Selects an autonomous sequence to run.
     * 
     * @param sequence the autonomous sequence to run
     */
    public void selectSequence(AutoSequence sequence) {
        selectedSequence = sequence;
    }

    /**
     * Returns the currently selected autonomous sequence.
     * 
     * @return the selected sequence - null means no sequence.
     */
    public AutoSequence getSelectedSequence() {
        return selectedSequence;
    }

    /**
     * Starts the selected autonomous sequence.
     */
    public void start() {
        drive.zero();
        if (selectedSequence != null) {
            System.out.println("[Auto] Starting auto sequence '" + selectedSequence.getName() + "'...");
            selectedSequence.run();
        }
    }

    /**
     * Stops the selected autonomous sequence.
     */
    public void stop() {
        if (selectedSequence != null) {
            System.out.println("[Auto] Stopping auto sequence '" + selectedSequence.getName() + "'...");
            selectedSequence.stop();
        }
    }

    /**
     * Updates the selected autonomous sequence.
     * 
     * @param timestamp
     */
    public void onLoop(double timestamp) {
        if (selectedSequence != null) {
            selectedSequence.onLoop(timestamp);
            // System.out.println(drive.getPigeonValue());
        }
    }
}