//enum ripped from Tone file at suggestion of Cordell Andersen, this was coded by Nathan Williams and lightly modified by Andrew Kerwin
public enum NoteLength {
    
		//valid lengths of notes
    	WHOLE(1.000f),
    	HALF(0.500f),
    	QUARTER(0.250f),
    	EIGTH(0.125f),
    	
    	//additional for error handling purposes
    	INVALID(0.0000f),
    	REST(0.000f);
    
    	//length of the note, class variable
    	private final int timeMs;

    	//quantifies the variable above
    	private NoteLength(float length) {
        	timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    	}
    	
    	//returns above variable for use
    	public int timeMs() {
        	return timeMs;
    	}
	}