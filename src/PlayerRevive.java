import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

//player class from moodle, heavily revised, base class by Nathan Williams and edited by Andrew Kerwin with assistance from Cordell Andersen
public class PlayerRevive implements Runnable {
	
	//ArrayLists created as class variables
	static ArrayList<NoteLength> lengths;
	
	//also, sequence to ensure the players will play in orderly fashion
	static ArrayList<Integer> noteSequence;
	
	//loadnotes method, creates players and gives them their bellnotes (note + length)
	private static ArrayList<PlayerRevive> loadNotes(String filename) {
		
		//player creation
		ArrayList<PlayerRevive> players = new ArrayList<PlayerRevive>();
		
		//instantiating previous class variables
		noteSequence = new ArrayList<>();
		lengths = new ArrayList<NoteLength>();
		
		//create the file instance to read
		final File file = new File(filename);
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader(fileReader)) {
                String line = null;
                
                //this while loop ensures the note read in from the file is not null
                while ((line = br.readLine()) != null) {
                    
                	//reads the line as a BellNote
                	BellNote bn = parseBellNote(line);
                	
                	//error checking along with BellNote assignment to players
                    if (bn != null) {
                    	
                    	//checks to see if we already have the player in question
                    	boolean wasADupe = false;
                    	for (int i = 0; i < players.size(); i++) {
                    		if (bn.note.equals(players.get(i).playerNote)) {
                    			wasADupe = true;
                    			lengths.add(bn.length);
                            	noteSequence.add(i);
                    		}
                    	}
                    	
                    	//otherwise, creates the player for the note
                    	if (!wasADupe) {
                    		PlayerRevive recruit = new PlayerRevive(bn);
                    		players.add(recruit);
                    		lengths.add(bn.length);
                        	noteSequence.add(players.size() - 1);
                    	}
                    } else {
                        System.err.println("Error: Invalid note '" + line + "'");
                    }
                }
            } catch (IOException ignored) {}
        } else {
            System.err.println("File '" + filename + "' not found");
        }
        return players;
    }
	
	private static Note parseNote(String note) {
        // If you give me garbage, I'll give it back
		// error checking
        if (note == null) {
            return Note.INVALID;
        }
        try {
        	//reads in the note on the line
            return Note.valueOf(note);
        } catch (IllegalArgumentException e) {
            return Note.INVALID;
        }
    }
	
	//our parse method to verify valid note lengths
    private static NoteLength parseLength(String num) {
    	//if statement to read in Rests appropriately
    	if (num.equals("REST")) {
    		return NoteLength.REST;
    	}
    		//variable that will serve as our length for the note, this can change, thus it is in a switch case per instance
    		NoteLength lengthCase;
        	switch(num) {
        	//numbers are 1/n
        	case "8" :
        		lengthCase = NoteLength.EIGTH;
        		break;
        	case "4" :
        		lengthCase = NoteLength.QUARTER;
    			break;
        	case "2" :
        		lengthCase = NoteLength.HALF;
    			break;
        	case "1" :
        		lengthCase = NoteLength.WHOLE;
    			break;
        	default  :
        		lengthCase = NoteLength.INVALID;
        		
        		//very descriptive error message in case we dont get valid notes
        		System.err.println("REEEEEEEEEEEEEEEEEEEEEEEEEEE");
          
          //we now have our length's value for the instance, return it for playing!
        } return lengthCase;
    }
    
    //puts together the bellnote using the previous methods
    private static BellNote parseBellNote(String line) {
    	
    	//checks for spacing
        String[] fields = line.split("\\s+");
        if (fields.length == 2) {
        	
           //note and length are read in
           return new BellNote(parseNote(fields[0]), parseLength(fields[1]));
        }
        return null;
    }
    
/*
    void playSong(ArrayList<PlayerRevive> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn: song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }
*/
   
    //BellNote bn = new BellNote();
    
    
    //also, passed the values of i from the for loop in run since it cannot be defined as class variables
    private void playNote(int i, SourceDataLine line) {
    	
    	//the mathematics for actually playing the note through our speakers
    	//this gets the value of the length at our current position in the array for playing
        final int ms = Math.min(lengths.get(i).timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        
        //this gets our note from the constructor
        line.write(playerNote.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    
    }
    
    //class variable made visible for main
    static SourceDataLine line;
    
	public static void main(String[] args) {
		
		//runs loadnotes so that players are created and assigned what they are playing
		ArrayList<PlayerRevive> recruit = loadNotes(args[0]);
		
		//instantiates af
    	final AudioFormat af =
            new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);

    	//starts up our player threads
    	for (PlayerRevive p: recruit) {
			p.startPlayer();
		}
		
    	//opens our sourcedataline to begin file reading
		try {
			line = AudioSystem.getSourceDataLine(af);
            line.open();
            line.start();
            
            //this loop goes through and gives players their turn to play
            for(int i = 0; i < lengths.size(); i++){
    			
            	//sets the value of i so we know where in our sequence we are, keeps players playing in order
            	setIntI(i);
            	
            	//player gets his turn based on position in sequence
            	recruit.get(noteSequence.get(i)).giveTurn();
            }
            line.drain();
		} catch (LineUnavailableException e) {
			System.out.println("Failed to open line");
		}
		
		//stops our players so program does not continue indefinitely
		for (PlayerRevive p: recruit){
			p.stopPlayer();
		}
		
		//waits for player termination
		for (PlayerRevive p: recruit) {
			p.waitToStop();
		}
	
	}
	
	private void waitToStop() {
			try {
				//our thread which is defined below
				t.join();
			} catch (InterruptedException e) {
				System.err.println(t.getName() + " really wants to play!");
			}
	}

	//class variables for other methods, our thread, boolean for run method, turn for the player, and the player's note
	private final Thread t;
	private volatile boolean running;
	private boolean myTurn;
	private Note playerNote;
	
	//PlayerRevive's constructor
	PlayerRevive(BellNote playerBellNote) {
		
		//gets the note from the bellnote to give to the player
		playerNote = playerBellNote.note;
		
		//player thread created!
		t = new Thread(this);
	}
	
	//starts up the player threads
	public void startPlayer() {
		
		//sets the boolean, players are allowed to run
		running = true;
		
		//threads begin
		t.start();
	}
	
	//stops the player threads
	public void stopPlayer() {
		
		//sets the boolean, players can go home
		running = false;
		synchronized(this) {
			notify();
		}
	}
	
	//players are given the ability to play, yay!
	public void giveTurn() {
		
		//synchronized to prevent spontaneous harmonies
		synchronized (this) {
			
			//see throw below
			if (myTurn) {
				throw new IllegalStateException("Attempt to give a turn to a player who hasn't completed the current turn.");
			}
			
			//player is given a turn
			myTurn = true;
			
			//they gonna play ALONE
			notify();
			
			//wait till other player's turns done
			while (myTurn) {
				try {
					wait();
				} catch (InterruptedException ignored) {}
			}
		}
	}
	
	//sing us a song, youre the piano man
	public void run() {
		
		//sing us a song tonight
		running = true;
		
		//synchronized so players wait their turn
		synchronized (this) {
			do {
				while (!myTurn && running) {
					try {
						wait();
					} catch (InterruptedException ignored) {}
				}
				
				//play the song
				if (running) {
					playNote(getIntI(), line);
					
				//signals next player to go 
				myTurn = false;
				notify();
				}
			} while (running);
		}
	}

	//variable for array position
	public static int passableValueOfIInList;
	
	//set method for value of i
	public static void setIntI(int i) {
		passableValueOfIInList = i;
	}
	
	//returns position in array
	public int getIntI() {
		return passableValueOfIInList;
	}
	
}