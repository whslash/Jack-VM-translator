//Huai Wu
//UIN: 825000258
//Project 7 VM translator

import java.io.*;
import java.util.*;

class Parser {
	private InputStream f;
	private Scanner s;
	private String currentCommand;
	private static String[] arithmetic = {"add","sub","neg","eq","gt","lt","and","or","not"};
	private String commandtype;
	private String firstArg;
	private int secondArg;
	private String[] token;
//open the vm file
	public Parser(String fileName) {
		try {
			f = new FileInputStream(fileName);
			s = new Scanner(f);
		} 
		catch (FileNotFoundException e) {
            System.out.println("No such file.");
        }
	}
//more commands?	
	public boolean hasMoreCommands() {
		return s.hasNextLine();
	}
//set commandtype, firstArg, and secondArg
	public void advance() {
	//delete comments
		String originLine = s.nextLine();
		int commentPosition = -1;
		commentPosition = originLine.indexOf("//");
		if (commentPosition != -1) currentCommand = originLine.substring(0,commentPosition);
		else currentCommand = originLine;
	//delete whitespaces	
		currentCommand = currentCommand.trim();
	//skip empty line
		if (currentCommand.isEmpty()) {
			commandtype = "NULL";
			return;
		}
	//get each token of currentCommand
		token = currentCommand.split("\\s+");
	
		List<String> list = Arrays.asList(arithmetic);
		if(list.contains(token[0])){
			commandtype = "C_ARITHMETIC";
			firstArg = token[0];
		}
		
		else {
			switch(token[0]) {
				case "push"    : commandtype = "C_PUSH"; firstArg = token[1]; secondArg = Integer.parseInt(token[2]); break;
				case "pop"     : commandtype = "C_POP";  firstArg = token[1]; secondArg = Integer.parseInt(token[2]); break;
				case "label"   : commandtype = "C_LABEL";  firstArg = token[1]; break;
				case "goto"    : commandtype = "C_GOTO";  firstArg = token[1]; break;
				case "if-goto" : commandtype = "C_IF";  firstArg = token[1]; break;
				case "function": commandtype = "C_FUNCTION";  firstArg = token[1]; secondArg = Integer.parseInt(token[2]); break;
				case "return"  : commandtype = "C_RETURN"; firstArg = token[0]; break;
				case "call"	   : commandtype = "C_CALL";  firstArg = token[1]; secondArg = Integer.parseInt(token[2]); break;
				default        : break;
			}
		}
	}
	
	public String commandType() {
		return commandtype;
	}
	
	public String arg1() {
		return firstArg;
	}
	
	public int arg2() {
		return secondArg;
	}
}

//-----------------------------------------------------------------------------------------------

class CodeWriter {
	private PrintWriter writer;
	private String outName;
	private int arithmeticNum, returnNum;
//create an output asm file
	public CodeWriter(String outName) {
		try {
			writer = new PrintWriter(outName);
		}
		catch (FileNotFoundException e) {
            System.out.println("Error.");
        }
	}
	
	public void setFileName(String fileName) {
		outName = fileName;
	}

//arithmetic commands translated	
	public void writeArithmetic(String command) {
		switch(command) {
			case "add" : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nM=M+D\n"); break;
			case "sub" : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nM=M-D\n"); break;
			case "neg" : writer.print("@SP\nA=M-1\nD=0\nM=D-M\n"); break;
			case "eq"  : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n"+ 
									 "@go"+arithmeticNum+"\nD;JEQ\n@SP\nA=M-1\nM=0\n"+
									 "@END"+arithmeticNum+"\n0;JMP\n(go"+arithmeticNum+")\n@SP\nA=M-1\nM=-1\n(END"+arithmeticNum+")\n"); arithmeticNum++; break;
			case "gt"  : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n"+ 
									 "@go"+arithmeticNum+"\nD;JGT\n@SP\nA=M-1\nM=0\n"+
									 "@END"+arithmeticNum+"\n0;JMP\n(go"+arithmeticNum+")\n@SP\nA=M-1\nM=-1\n(END"+arithmeticNum+")\n"); arithmeticNum++; break;
			case "lt"  : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n"+ 
									 "@go"+arithmeticNum+"\nD;JLT\n@SP\nA=M-1\nM=0\n"+
									 "@END"+arithmeticNum+"\n0;JMP\n(go"+arithmeticNum+")\n@SP\nA=M-1\nM=-1\n(END"+arithmeticNum+")\n"); arithmeticNum++; break;
			case "and" : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nM=M&D\n"); break;
			case "or"  : writer.print("@SP\nAM=M-1\nD=M\nA=A-1\nM=M|D\n"); break;
			case "not" : writer.print("@SP\nA=M-1\nM=!M\n"); break;
			default: break;
		}
	}

//push pop commands translated	 
	public void writePushPop(String command, String segment, int index) {
		if(command.equals("C_PUSH")) {
			switch(segment) {
				case "argument" : writer.print("@ARG\nD=M\n@"+index+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "local"    : writer.print("@LCL\nD=M\n@"+index+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "static"   : writer.print("@"+outName.replace('/','.')+"."+index+"\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "constant" : writer.print("@"+index+"\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "this"		: writer.print("@THIS\nD=M\n@"+index+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "that"		: writer.print("@THAT\nD=M\n@"+index+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "pointer"	: if (index==0) writer.print("@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
								  else if (index==1) writer.print("@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				case "temp"		: writer.print("@R5\nD=A\n@"+index+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");break;
				default         : break;
			}
		}
		else if(command.equals("C_POP")) {
			switch(segment) {
				case "argument" : writer.print("@ARG\nD=M\n@"+index+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");break;
				case "local"    : writer.print("@LCL\nD=M\n@"+index+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");break;
				case "static"   : writer.print("@"+outName.replace('/','.')+"."+index+"\nD=A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");break;
				case "this"     : writer.print("@THIS\nD=M\n@"+index+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");break;
				case "that"     : writer.print("@THAT\nD=M\n@"+index+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");break;
				case "pointer"  : if(index==0) writer.print("@SP\nAM=M-1\nD=M\n@R3\nM=D\n");
								  else if(index==1) writer.print("@SP\nAM=M-1\nD=M\n@R4\nM=D\n");break;
				case "temp"		: writer.print("@R5\nD=A\n@"+index+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");break;
				default         : break;
			}
		}
	}

//call if there're multiple functions	
	public void writeInit() {
		writer.print("@256\nD=A\n@SP\nM=D\n");
        writeCall("Sys.init",0);
	}
	
	public void writeLabel(String label) {
		writer.print("("+label+")\n");
	}
	
	public void writeGoto(String label){
        writer.print("@"+label+"\n0;JMP\n");
    }
	
	public void writeIf(String label){
        writer.print("@SP\nAM=M-1\nD=M\nA=A-1\n@"+label+"\nD;JNE\n");
    }

	public void writeCall(String functionName, int numArgs){
        writer.print("@RETURN_LABEL"+returnNum+"\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"	//push return address
                     +"@LCL\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n@ARG\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"	//push LCL, ARG, THIS, THAT
					 +"@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"
					 +"@SP\nD=M\n@5\nD=D-A\n@"+numArgs+"\nD=D-A\n@ARG\nM=D\n@SP\nD=M\n@LCL\nM=D\n@"		//ARG=SP-n-5, LCL=SP
				   	 +functionName+"\n0;JMP\n(RETURN_LABEL"+returnNum+")\n");	//goto f, (return address)
		returnNum++;
    }
	
	public void writeReturn(){
        writer.print("@LCL\nD=M\n@R5\nM=D\n@5\nA=D-A\nD=M\n@R6\nM=D\n"		//FRAME=LCL, RET=*(FRAME-5)
					 +"@ARG\nD=M\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n@ARG\nD=M\n@SP\nM=D+1\n"		//*ARG=pop(), SP=ARG+1
					 +"@R5\nD=M-1\nAM=D\nD=M\n@THAT\nM=D\n@R5\nD=M-1\nAM=D\nD=M\n@THIS\nM=D\n"		//THAT=*(FRAME-1), THIS=*(FRAME-2)
                     +"@R5\nD=M-1\nAM=D\nD=M\n@ARG\nM=D\n@R5\nD=M-1\nAM=D\nD=M\n@LCL\nM=D\n"		//ARG=*(FRAME-3), LCL=*(FRAME-4)
					 +"@R6\nA=M\n0;JMP\n");		//goto RET
    }
	
	public void writeFunction(String functionName, int numLocals){
        writer.print("("+functionName+")\n");

        for (int i=0; i<numLocals; i++){
			writer.print("@0\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");	//initialize all to 0
        }
    }
	
	public void finish() {
		writer.close();
	}
}

//-----------------------------------------------------------------------------------------

public class VM {
	public static void main(String[] args) {
		System.out.println("Enter the path of your vm files:\n(e.g 'ProgramFlow/FibonacciSeries/', make sure there's a '/' at the end of the path)");
		Scanner reader = new Scanner(System.in);
		String path = reader.next();
		
	//only get files with extension ".vm"
		File[] files = new File(path).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".vm");
			}
		});
		
	//set path of output file
		File fileOut = new File(path+"output.asm");;
        CodeWriter writer = new CodeWriter(path+"output.asm");
		
	//call 	writeInit() if there is Sys.init
		if(files.length>1) {
			writer.writeInit();
		}
		else if(files.length ==0) {
			System.out.println("No vm file.");
			return;
		}
		
		String currenttype;
		for(File each : files) {
			String fileName = each.getAbsolutePath();
			Parser parser = new Parser(fileName);
			writer.setFileName(fileName);
			
			while (parser.hasMoreCommands()) {
				parser.advance();
				currenttype = parser.commandType();
				
			//write based on different command types
				switch(currenttype) {
					case "C_ARITHMETIC" : writer.writeArithmetic(parser.arg1()); break;
					case "C_POP" :
					case "C_PUSH" : writer.writePushPop(currenttype, parser.arg1(), parser.arg2()); break;
					case "C_LABEL": writer.writeLabel(parser.arg1()); break;
					case "C_GOTO" : writer.writeGoto(parser.arg1()); break;
					case "C_IF" :writer.writeIf(parser.arg1()); break;
					case "C_RETURN" :writer.writeReturn(); break;
					case "C_FUNCTION" :writer.writeFunction(parser.arg1(),parser.arg2()); break;
					case "C_CALL" : writer.writeCall(parser.arg1(),parser.arg2()); break;
					default : break;
				}
			}
		}
		writer.finish();
		System.out.println("Translated successfully.\nFile name: 'output.asm'");
	}
}





