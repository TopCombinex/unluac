package unluac;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;


import unluac.Configuration.Mode;
import unluac.assemble.Assembler;
import unluac.assemble.AssemblerException;
import unluac.decompile.Decompiler;
import unluac.decompile.Disassembler;
import unluac.decompile.Output;
import unluac.decompile.OutputProvider;
import unluac.parse.BHeader;
import unluac.parse.LFunction;

public class Main {

    public static String version = "1.2.3.451";

    public static void main(String[] args) {
        String fn = null;
        Configuration config = new Configuration();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                // option
                switch (arg) {
                    case "--rawstring":
                        config.rawstring = true;
                        break;
                    case "--nodebug":
                        config.variable = Configuration.VariableMode.NODEBUG;
                        break;
                    case "--disassemble":
                        config.mode = Mode.DISASSEMBLE;
                        break;
                    case "--assemble":
                        config.mode = Mode.ASSEMBLE;
                        break;
                    case "--output":
                    case "-o":
                        if (i + 1 < args.length) {
                            config.output = args[i + 1];
                            i++;
                        } else {
                            error("option \"" + arg + "\" doesn't have an argument", true);
                        }
                        break;
                    case "--opmap":
                        if (i + 1 < args.length) {
                            config.opmap = args[i + 1];
                            i++;
                        } else {
                            error("option \"" + arg + "\" doesn't have an argument", true);
                        }
                        break;
                    default:
                        error("unrecognized option: " + arg, true);
                        break;
                }
            } else if (fn == null) {
                fn = arg;
            } else {
                error("too many arguments: " + arg, true);
            }
        }
        if (fn == null) {
            error("no input file provided", true);
        } else {
            switch (config.mode) {
                case DECOMPILE: {
                    LFunction lmain = null;
                    try {
                        lmain = file_to_function(fn, config);
                    } catch (IOException e) {
                        error(e.getMessage(), false);
                    }
                    Decompiler d = new Decompiler(lmain);
                    Decompiler.State result = d.decompile();
                    d.print(result);
                    break;
                }
                case DISASSEMBLE: {
                    LFunction lmain = null;
                    try {
                        lmain = file_to_function(fn, config);
                    } catch (IOException e) {
                        error(e.getMessage(), false);
                    }
                    Disassembler d = new Disassembler(lmain);
                    d.disassemble(new Output());
                    break;
                }
                case ASSEMBLE: {
                    if (config.output == null) {
                        error("assembler mode requires an output file", true);
                    } else {
                        try {
                            Assembler a = new Assembler(
                                    new BufferedReader(new FileReader(new File(fn))),
                                    new FileOutputStream(config.output)
                            );
                            a.assemble();
                        } catch (IOException | AssemblerException e) {
                            error(e.getMessage(), false);
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalStateException();
            }
            System.exit(0);
        }
    }

    private static void error(String err, boolean usage) {
        System.err.println("unluac v" + version);
        System.err.print("  error: ");
        System.err.println(err);
        if (usage) {
            System.err.println("  usage: java -jar unluac.jar [options] <file>");
        }
        System.exit(1);
    }

    private static LFunction file_to_function(String fn, Configuration config) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(fn, "r")) {
            ByteBuffer buffer = ByteBuffer.allocate((int) file.length());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int len = (int) file.length();
            FileChannel in = file.getChannel();
            while (len > 0) len -= in.read(buffer);
            buffer.rewind();
            BHeader header = new BHeader(buffer, config);
            return header.main;
        }
    }

    public static void decompile(String in, String out) throws IOException {
        LFunction lmain = file_to_function(in, new Configuration());
        Decompiler d = new Decompiler(lmain);
        Decompiler.State result = d.decompile();
        final PrintStream pout = new PrintStream(out);
        d.print(result, new Output(new OutputProvider() {

            @Override
            public void print(String s) {
                pout.print(s);
            }

            @Override
            public void print(byte b) {
                pout.print(b);
            }

            @Override
            public void println() {
                pout.println();
            }

        }));
        pout.flush();
        pout.close();
    }

    public static void assemble(String in, String out) throws IOException, AssemblerException {
        OutputStream outstream = new BufferedOutputStream(new FileOutputStream(new File(out)));
        Assembler a = new Assembler(new BufferedReader(new FileReader(new File(in))), outstream);
        a.assemble();
        outstream.flush();
        outstream.close();
    }

    public static void disassemble(String in, String out) throws IOException {
        LFunction lmain = file_to_function(in, new Configuration());
        Disassembler d = new Disassembler(lmain);
        final PrintStream pout = new PrintStream(out);
        d.disassemble(new Output(new OutputProvider() {

            @Override
            public void print(String s) {
                pout.print(s);
            }

            @Override
            public void print(byte b) {
                pout.print(b);
            }

            @Override
            public void println() {
                pout.println();
            }

        }));
        pout.flush();
        pout.close();
    }

}
