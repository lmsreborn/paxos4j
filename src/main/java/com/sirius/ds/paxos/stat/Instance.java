package com.sirius.ds.paxos.stat;

import com.google.common.base.MoreObjects;
import com.sirius.ds.paxos.PeerID;
import com.sirius.ds.paxos.msg.VersionedData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public abstract class Instance implements StatMachine {

    transient AtomicBoolean committed = new AtomicBoolean(false);
    transient ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    transient Consumer<InstanceStatus> watcher = null;

    public Instance() {

    }

    public Instance(long instanceId, VersionedData acceptData) {
        this.instanceId = instanceId;
        this.acceptData = acceptData;
    }

    public void clean() {
        rwl = null;
        watcher = null;
    }

    long instanceId;
    InstanceStatus status = InstanceStatus.INIT;
    int promisedBallot = 0;
    int acceptBallot = 0;
    VersionedData acceptData = null;
    Set<PeerID> prepared = new HashSet<>();
    Set<PeerID> accepted = new HashSet<>();

    public long getInstanceId() {
        return instanceId;
    }

    public InstanceStatus getStatus() {
        if (committed.get()) {
            return status;
        }

        rwl.readLock().lock();
        try {
            return status;
        } finally {
            rwl.readLock().unlock();
        }
    }

    public void setStatus(InstanceStatus status) {
        rwl.writeLock().lock();
        try {
            this.status = status;

            if (watcher != null) {
                watcher.accept(status);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public int getPromisedBallot() {
        if (committed.get()) {
            return promisedBallot;
        }

        rwl.readLock().lock();
        try {
            return promisedBallot;
        } finally {
            rwl.readLock().unlock();
        }
    }

    public VersionedData getAcceptData() {
        if (committed.get()) {
            return acceptData;
        }

        rwl.readLock().lock();
        try {
            return acceptData;
        } finally {
            rwl.readLock().unlock();
        }
    }

    public Set<PeerID> getPrepared() {
        return Collections.unmodifiableSet(prepared);
    }

    public Set<PeerID> getAccepted() {
        return Collections.unmodifiableSet(accepted);
    }

    public boolean isCommitted() {
        return committed.get();
    }

    @Override
    public void registerStatusWatcher(Consumer<InstanceStatus> watcher) {
        this.watcher = watcher;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("")
                .add("instanceId", instanceId)
                .add("committed", committed.get())
                .add("status", status)
                .add("promisedBallot", promisedBallot)
                .add("acceptBallot", acceptBallot)
                .add("acceptData",
                        acceptData == null
                        ? null
                        : String.format("[%s=%s]", acceptData.getKey(), Arrays.toString(acceptData.getPayload())))
//                .add("prepared", prepared)
//                .add("accepted", accepted)
                .toString();
    }
}
