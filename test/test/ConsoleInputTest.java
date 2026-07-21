package test;

import exception.EndOfInputException;
import exception.OperationCancelledException;
import org.junit.Test;
import view.ConsoleInput;

import java.util.Scanner;

import static org.junit.Assert.assertThrows;

public class ConsoleInputTest {
    @Test
    public void readLine_EndOfStream_ThrowsDedicatedSignal() {
        ConsoleInput input = new ConsoleInput(new Scanner(""));
        assertThrows(EndOfInputException.class, () -> input.readLine("Prompt: "));
    }

    @Test
    public void readLine_CancelKeyword_StillCancelsNormalOperation() {
        ConsoleInput input = new ConsoleInput(new Scanner("cancel\n"));
        assertThrows(OperationCancelledException.class, () -> input.readLine("Prompt: "));
    }
}
