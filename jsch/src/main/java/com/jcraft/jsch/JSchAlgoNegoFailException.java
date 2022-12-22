package com.jcraft.jsch;

/**
 * Extension of {@link JSchException} to indicate when a connection fails during algorithm
 * negotiation.
 */
public class JSchAlgoNegoFailException extends JSchException {
    private static final long serialVersionUID = -1L;

    private final String algorithmName;
    private final String clientProposal;
    private final String serverProposal;

    JSchAlgoNegoFailException(final int algorithmIndex,
                              final String clientProposal, final String serverProposal) {
        super(failString(algorithmIndex,
                clientProposal, serverProposal));
        algorithmName = KeyExchange.getAlgorithmNameByProposalIndex(algorithmIndex);
        this.clientProposal = clientProposal;
        this.serverProposal = serverProposal;
    }

    /**
     * Get the algorithm name.
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Get the client algorithm proposal.
     */
    public String getClientProposal() {
        return clientProposal;
    }

    /**
     * Get the server algorithm proposal.
     */
    public String getServerProposal() {
        return serverProposal;
    }

    private static String failString(final int algorithmIndex,
                                     final String clientProposal, final String serverProposal) {
        return String.format(
                "Algorithm negotiation fail:\nalgorithmName=\"%s\"\nclientProposal=\"%s\"\nserverProposal=\"%s\"",
                KeyExchange.getAlgorithmNameByProposalIndex(algorithmIndex),
                clientProposal, serverProposal);
    }
}
