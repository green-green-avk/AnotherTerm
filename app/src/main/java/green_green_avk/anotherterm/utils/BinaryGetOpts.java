package green_green_avk.anotherterm.utils;

import java.util.HashMap;
import java.util.Map;

// Why existing implementations are working with strings?
public final class BinaryGetOpts {
    public static final class ParseException extends Exception {
        public ParseException(final String message) {
            super(message);
        }
    }

    private static final int MAX_ARG_NAME_LEN = 32;

    public static final class Option {
        public enum Type {NONE, STRING, INT, BIN}

        public final String key;
        public final String[] names;
        public final Type type;

        public Option(final String key, final String[] names, final Type type) {
            this.key = key;
            this.names = names;
            this.type = type;
        }
    }

    public static final class Options {
        private final Map<String, Option> map = new HashMap<>();
        public final int maxNameLength;

        public Options(final Option[] list) {
            int maxLen = 0;
            for (final Option opt : list) {
                for (final String name : opt.names) {
                    map.put(name, opt);
                    if (name.length() > maxLen) maxLen = name.length();
                }
            }
            maxNameLength = maxLen;
        }

        public Option get(final String name) {
            return map.get(name);
        }
    }

    public static final class Parser {
        private final byte[][] args;
        public int position = 0;

        public Parser(final byte[][] args) {
            this.args = args;
        }

        public void skip() {
            if (position < args.length) ++position;
        }

        public Map<String, ?> parse(final Options options) throws ParseException {
            final Map<String, Object> ret = new HashMap<>();
            for (; position < args.length; ++position) {
                final byte[] arg = args[position];
                if (arg.length > options.maxNameLength) break; // ASCII only
                final String strArg = Misc.fromUTF8(arg);
                if ("--".equals(strArg)) {
                    ++position;
                    break;
                }
                final Option opt = options.get(strArg);
                if (opt == null) break;
                final Object val;
                if (opt.type == Option.Type.NONE) {
                    val = true;
                } else if (position + 1 < args.length) {
                    ++position;
                    switch (opt.type) {
                        case STRING:
                            val = Misc.fromUTF8(args[position]);
                            break;
                        case INT:
                            try {
                                val = Integer.parseInt(Misc.fromUTF8(args[position]));
                            } catch (final NumberFormatException e) {
                                throw new ParseException(String.format(
                                        "%s argument is not an integer", strArg));
                            }
                            break;
                        case BIN:
                            val = args[position];
                            break;
                        default:
                            val = null;
                    }
                } else {
                    throw new ParseException(String.format(
                            "%s argument value is missing", strArg));
                }
                ret.put(opt.key, val);
            }
            return ret;
        }
    }
}
