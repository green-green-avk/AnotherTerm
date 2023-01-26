/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2012-2018 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

final class LocalIdentityRepository implements IdentityRepository {
    private static final String name = "Local Identity Repository";

    private final List<Identity> identities = new ArrayList<>();
    private final JSch jsch;

    LocalIdentityRepository(final JSch jsch) {
        this.jsch = jsch;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getStatus() {
        return RUNNING;
    }

    @Override
    public synchronized List<Identity> getIdentities() {
        removeDuplicates();
        return new ArrayList<>(identities);
    }

    public synchronized void add(final Identity identity) {
        if (!identities.contains(identity)) {
            final byte[] blob1 = identity.getPublicKeyBlob();
            if (blob1 == null) {
                identities.add(identity);
                return;
            }
            final Iterator<Identity> it = identities.iterator();
            while (it.hasNext()) {
                final Identity id = it.next();
                final byte[] blob2 = id.getPublicKeyBlob();
                if (blob2 != null && Arrays.equals(blob1, blob2)) {
                    if (!identity.isEncrypted() && id.isEncrypted()) {
                        it.remove();
                    } else {
                        return;
                    }
                }
            }
            identities.add(identity);
        }
    }

    @Override
    public synchronized boolean add(final byte[] identity) {
        try {
            add(IdentityFile.newInstance("remote key", identity, null, jsch));
            return true;
        } catch (final JSchException e) {
            return false;
        }
    }

    synchronized void remove(final Identity identity) {
        if (identities.contains(identity)) {
            identities.remove(identity);
            identity.clear();
        } else {
            remove(identity.getPublicKeyBlob());
        }
    }

    @Override
    public synchronized boolean remove(final byte[] blob) {
        if (blob == null)
            return false;
        final Iterator<Identity> it = identities.iterator();
        while (it.hasNext()) {
            final Identity id = it.next();
            final byte[] ib = id.getPublicKeyBlob();
            if (ib == null || !Arrays.equals(blob, ib))
                continue;
            it.remove();
            id.clear();
            return true;
        }
        return false;
    }

    @Override
    public synchronized void removeAll() {
        for (final Identity id : identities) {
            id.clear();
        }
        identities.clear();
    }

    // Taking into account low number identities and significant price of objects in Java...
    // Let it be O(n^2)
    private void removeDuplicates() {
        if (identities.isEmpty())
            return;
        final ListIterator<Identity> it = identities.listIterator();
        while (it.hasNext()) {
            final Identity id1 = it.next();
            final byte[] blob1 = id1.getPublicKeyBlob();
            if (blob1 == null)
                continue;
            for (int ii = it.nextIndex(); ii < identities.size(); ii++) {
                final Identity id2 = identities.get(ii);
                final byte[] blob2 = id2.getPublicKeyBlob();
                if (Arrays.equals(blob2, blob1) &&
                        id2.isEncrypted() == id1.isEncrypted()) {
                    it.remove();
                    break;
                }
            }
        }
    }
}
