package com.jcraft.jsch;

public class JSchNotImplementedException extends JSchException {
    public JSchNotImplementedException() {
    }

    public JSchNotImplementedException(final String s) {
        super(s);
    }

    public JSchNotImplementedException(final String s, final Throwable e) {
        super(s, e);
    }

    public static JSchNotImplementedException forFeature(final String featureName) {
        return new JSchNotImplementedException("Unable to load class for '" + featureName + "'");
    }

    public static JSchNotImplementedException forFeature(final String featureName,
                                                         final Throwable e) {
        return new JSchNotImplementedException("Unable to load class for '" + featureName + "': "
                + e.getLocalizedMessage(), e);
    }
}
