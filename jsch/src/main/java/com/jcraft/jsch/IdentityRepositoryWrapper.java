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
import java.util.Iterator;
import java.util.List;

/**
 * JSch will accept ciphered keys, but some implementations of
 * IdentityRepository can not.  For example, IdentityRepository for
 * ssh-agent and pageant only accept plain keys.  The following class has
 * been introduced to cache ciphered keys for them, and pass them
 * whenever they are de-ciphered.
 */
final class IdentityRepositoryWrapper implements IdentityRepository {
    private final IdentityRepository wrapped;
    private final List<Identity> cache = new ArrayList<>();
    private final boolean keepInCache;

    IdentityRepositoryWrapper(final IdentityRepository wrapped) {
        this(wrapped, false);
    }

    IdentityRepositoryWrapper(final IdentityRepository wrapped, final boolean keepInCache) {
        this.wrapped = wrapped;
        this.keepInCache = keepInCache;
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public int getStatus() {
        return wrapped.getStatus();
    }

    @Override
    public boolean add(final byte[] identity) {
        return wrapped.add(identity);
    }

    @Override
    public boolean remove(final byte[] blob) {
        return wrapped.remove(blob);
    }

    @Override
    public void removeAll() {
        cache.clear();
        wrapped.removeAll();
    }

    @Override
    public List<Identity> getIdentities() {
        final List<Identity> result = wrapped.getIdentities();
        result.addAll(cache);
        return result;
    }

    private boolean needAddToWrapped(final Identity identity) {
        return !identity.isEncrypted() && (identity instanceof IdentityFile);
    }

    private void addToWrapped(final Identity identity) {
        try {
            wrapped.add(((IdentityFile) identity).getKeyPair().forSSHAgent());
        } catch (final JSchException e) {
            // an exception will not be thrown.
        }
    }

    void add(final Identity identity) {
        if (!keepInCache && needAddToWrapped(identity))
            addToWrapped(identity);
        else
            cache.add(identity);
    }

    void check() {
        if (!keepInCache && !cache.isEmpty()) {
            final Iterator<Identity> it = cache.iterator();
            while (it.hasNext()) {
                final Identity identity = it.next();
                if (needAddToWrapped(identity)) {
                    it.remove();
                    addToWrapped(identity);
                }
            }
        }
    }
}
