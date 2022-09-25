package com.jcraft.jsch;

/**
 * Extension of {@link JSchException} to indicate when a connection fails during algorithm
 * negotiation.
 */
public class JSchAlgoNegoFailException extends JSchException {

    private static final long serialVersionUID = -1L;

    private final String algorithmName;
    private final String jschProposal;
    private final String serverProposal;

    JSchAlgoNegoFailException(final int algorithmIndex,
                              final String jschProposal, final String serverProposal) {
        super(failString(algorithmIndex,
                jschProposal, serverProposal));
        algorithmName = KeyExchange.getAlgorithmNameByProposalIndex(algorithmIndex);
        this.jschProposal = jschProposal;
        this.serverProposal = serverProposal;
    }

    /**
     * Get the algorithm name.
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Get the JSch algorithm proposal.
     */
    public String getJSchProposal() {
        return jschProposal;
    }

    /**
     * Get the server algorithm proposal.
     */
    public String getServerProposal() {
        return serverProposal;
    }

    private static String failString(final int algorithmIndex,
                                     final String jschProposal, final String serverProposal) {
        return String.format(
                "Algorithm negotiation fail: algorithmName=\"%s\" jschProposal=\"%s\" serverProposal=\"%s\"",
                KeyExchange.getAlgorithmNameByProposalIndex(algorithmIndex),
                jschProposal, serverProposal);
    }
}
