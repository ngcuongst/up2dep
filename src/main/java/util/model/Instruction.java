package util.model;

import java.util.ArrayList;
import com.ibm.wala.ssa.SSAInstruction;

public abstract class Instruction {
    // reference to the SDGNode this instruction belongs to
    public SDGNode node;

    // WALA SSAInstruction reference
    public SSAInstruction ssaInstr;

    // the enclosing method (signature) to which this instruction belongs to
    public String bytecodeMethod = null;

    // destination value, if any
    public String destination = null;

    /**
     * Retrieves all source values/register of this instruction
     *
     * @return a list of values
     */
    public abstract ArrayList<String> getSources();


    /**
     * Removes the destination register from a call node label,
     * e.g. "v25 = v21.getSystemService(#(phone))" is transformed
     * to "v21.getSystemService(#(phone))"
     *
     * @param instruction
     * @return
     * @deprecated
     */
    public static String stripDestinationRegister(String instruction) {
        String[] fragments = instruction.split(" = ");
        if (fragments.length > 1 && !fragments[0].contains("<"))
            return instruction.substring(fragments[0].length() + " = ".length());
        else
            return instruction;
    }


    @Override
    public int hashCode() {
        return this.ssaInstr.hashCode() ^ this.bytecodeMethod.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        return this.ssaInstr.equals(((Instruction) o).ssaInstr) &&
                this.bytecodeMethod.equals(((Instruction) o).bytecodeMethod);
    }
}
