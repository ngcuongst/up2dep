package util;
import util.CodeUtil;
import util.model.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class CodeConverter {
    public static String generateSignature(String clazzName, String method, String returnType) {
        StringBuilder sb = new StringBuilder(clazzName + ".");

        String methodString = method.substring(0, method.indexOf("("));
        sb.append(methodString + "(");

        String[] args = method.substring(method.indexOf("(") + 1, method.indexOf(")")).split(",");
        for (String arg : args) {
            sb.append((arg));
        }

        sb.append(")");
        sb.append(CodeUtil.convertToDexBytecodeNotation(returnType));

        return sb.toString();
    }

    /**
     * Converts class name in dex bytecode notation to fully-qualified class name
     * @param className name in dex bytcode notation, e.g. "Lcom/ebay/motors/garage/myvehicles/GarageInsertActivity;"
     * @return className fully-qualified class name, e.g. "com.ebay.motors.garage.myvehicles.GarageInsertActivity"
     */
    public static String convertToFullClassName(String className) {
        if (className.endsWith(";")) {
            className = className.substring(0, className.length() - 1);
        }

        // convert array types
        while (className.startsWith("["))
            className = className.substring(1) + "[]";

        // remove type identifier
        if (className.startsWith("L"))
            className = className.replaceFirst("L", "");
        else if (VARTYPES.containsKey(className.charAt(0)) && className.length() > 1 && '[' == className.charAt(1)) { // single / multi-dimensional primitive array type
            className = VARTYPES.get(className.charAt(0)) + className.substring(1);
        }

        return className.replaceAll("/", "\\.");
    }


    public static String getSelector(String signature) {
        return signature.substring(signature.lastIndexOf(".") + 1);
    }

    /**
     * Returns the method name of a method signature
     * @param methodSignature
     * @return
     */
    public static String getMethodName(final String methodSignature) {
        String result = methodSignature.substring(0, methodSignature.indexOf("(")); // strip args and return type
        result = Instruction.stripDestinationRegister(result);
        int startIdx = result.lastIndexOf(".") == -1 ? 0 : result.lastIndexOf(".") + 1;
        return result.substring(startIdx);
    }

    // TODO: joana signature is inconsistent (only return type is in dex notation),
    //        see "java.lang.StringBuilder.toString()Ljava/lang/String;"
    // @ToDo: Primitive Arrays are broken?
    public static String getReturnType(final String methodSignature, boolean bytecodeNotation) {
        final String retType = methodSignature.substring(methodSignature.lastIndexOf(")") + 1); // strip anything but the return type

        if (retType.length() == 1 && VARTYPES.containsKey(retType.charAt(0))) {
            return VARTYPES.get(retType.charAt(0));
        } else {
            return bytecodeNotation ? retType : convertToFullClassName(retType);
        }
    }

    /**
     * Vartypes used in Dex bytecode and their mnemonics
     */
    public static final HashMap<Character, String> VARTYPES = new HashMap<Character, String>() {
        private static final long serialVersionUID = 1L;

        {
            put('V', "void");    // can only be used for return types
            put('Z', "boolean");
            put('B', "byte");
            put('S', "short");
            put('C', "char");
            put('I', "int");
            put('J', "long");    // 64 bits
            put('F', "float");
            put('D', "double");  // 64 bits
        }
    };

    /**
     * Mnemonics to dex bytecode vartypes
     */
    public static final HashMap<String, Character> TOVARTYPES = new HashMap<String, Character>() {
        private static final long serialVersionUID = 1L;

        {
            put("void",    'V');    // can only be used for return types
            put("boolean", 'Z');
            put("byte",    'B');
            put("short",   'S');
            put("char",    'C');
            put("int",     'I');
            put("long",    'J');    // 64 bits
            put("float",   'F');
            put("double",  'D');  // 64 bits
        }
    };


    public static boolean isPrimitiveType(String type) {
        return TOVARTYPES.containsKey(type) || VARTYPES.containsKey(type.charAt(0));
    }

    public static boolean isArrayType(String type) {
        return type.startsWith("[");
    }

    public static boolean isParameterRegister(String register) {
        return register.matches("^p\\d{1,4}$");
    }

    public static boolean isNormalRegister(String register) {
        return register.matches("^v\\d{1,5}$");
    }


    /**
     * Parses the method argument header of a dex method signature
     *
     * @param signature     method signature in dex notation
     * @param humanReadable if true it converts their dex vartypes to human readable types
     * @return an array of (human readable) argument types
     * @deprecated use IMethodReference directly instead of parsing ourselves (arguments are already parsed in CallInstructions)
     */
    @Deprecated
    // TODO: array args are not converted properly, i.e. [I -> [int  instead of  int[]
    public static List<String> parseMethodArguments(String signature, boolean humanReadable) {
        ArrayList<String> result = new ArrayList<String>();

        // Parse arguments
        String args = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));
        Boolean parsingObject = false;
        String currentStr = "";
        for (char c : args.toCharArray()) {
            if (c == 'L' && !parsingObject) { // start of class object
                parsingObject = true;
            } else if (c == ';') {  // end of class object
                parsingObject = false;
                result.add(humanReadable ? currentStr.replaceAll("/", ".") : currentStr);
                currentStr = "";
            } else if (VARTYPES.containsKey(c) && !parsingObject) {  // found var type
                result.add(currentStr + (humanReadable ? VARTYPES.get(c) : currentStr));
                currentStr = "";
            } else
                currentStr += c;
        }

        return result;
    }
}
