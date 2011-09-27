// Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon 30 Apr 2007 at 13:16:00 PST by lamport
//      modified on Tue May 15 11:44:57 PDT 2001 by yuanyu

package tlc2.tool.fp;

import java.io.IOException;
import java.rmi.RemoteException;

import tlc2.output.EC;
import tlc2.tool.TLCTrace;
import tlc2.util.BufferedRandomAccessFile;
import util.Assert;

/**
 * An <code>MultiFPSet</code> is a set of 64-bit fingerprints.
 */
public class MultiFPSet extends FPSet {

	private FPSet[] sets;
	private int fpbits;

	/* Create a MultiFPSet with 2^bits FPSets. */
	public MultiFPSet(int bits, long fpMemSize) throws RemoteException {
		int len = 1 << bits;
		this.sets = new FPSet[len];
		for (int i = 0; i < len; i++) {
			// this.sets[i] = new MemFPSet();
			this.sets[i] = new DiskFPSet((int) (fpMemSize / len));
		}
		this.fpbits = 64 - bits;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#init(int, java.lang.String, java.lang.String)
	 */
	public final void init(int numThreads, String metadir, String filename) throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].init(numThreads, metadir, filename + "_" + i);
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#size()
	 */
	public final long size() {
		/* Returns the number of fingerprints in this set. */
		int sum = 0;
		for (int i = 0; i < this.sets.length; i++) {
			sum += this.sets[i].size();
		}
		return sum;
	}

	/**
	 * Returns <code>true</code> iff the fingerprint <code>fp</code> is in this
	 * set. If the fingerprint is not in the set, it is added to the set as a
	 * side-effect.
	 * 
	 * @see tlc2.tool.fp.FPSet#put(long)
	 */
	public final boolean put(long fp) throws IOException {
		int idx = (int) (fp >>> this.fpbits);
		return this.sets[idx].put(fp);
	}

	/**
	 * Returns <code>true</code> iff the fingerprint <code>fp</code> is in this
	 * set.
	 * 
	 * @see tlc2.tool.fp.FPSet#contains(long)
	 */
	public final boolean contains(long fp) throws IOException {
		int idx = (int) (fp >>> this.fpbits);
		return this.sets[idx].contains(fp);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#close()
	 */
	public final void close() {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].close();
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#checkFPs()
	 */
	public final double checkFPs() throws IOException {
		/* This is not quite correct. */
		double res = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < this.sets.length; i++) {
			res = Math.max(res, this.sets[i].checkFPs());
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#exit(boolean)
	 */
	public final void exit(boolean cleanup) throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].exit(cleanup);
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#beginChkpt()
	 */
	public final void beginChkpt() throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].beginChkpt();
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#commitChkpt()
	 */
	public final void commitChkpt() throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].commitChkpt();
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#recover()
	 */
	public final void recover() throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].prepareRecovery();
		}

		long recoverPtr = TLCTrace.getRecoverPtr();
		BufferedRandomAccessFile braf = new BufferedRandomAccessFile(TLCTrace.getFilename(), "r");
		while (braf.getFilePointer() < recoverPtr) {
			braf.readLongNat(); /* drop */
			long fp = braf.readLong();
			int setIdx = (int) (fp >>> this.fpbits);
			this.sets[setIdx].recoverFP(fp);
		}

		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].completeRecovery();
		}
	}

	/* (non-Javadoc)
	 * 
	 * NOOP!
	 * 
	 * @see tlc2.tool.fp.FPSet#prepareRecovery()
	 */
	public final void prepareRecovery() throws IOException { /* SKIP */
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#recoverFP(long)
	 */
	public final void recoverFP(long fp) throws IOException {
		Assert.check(!this.put(fp), EC.TLC_FP_NOT_IN_SET);
	}

	/* (non-Javadoc)
	 * 
	 * NOOP!
	 * 
	 * @see tlc2.tool.fp.FPSet#completeRecovery()
	 */
	public final void completeRecovery() throws IOException { /* SKIP */
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#beginChkpt(java.lang.String)
	 */
	public final void beginChkpt(String filename) throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].beginChkpt(filename);
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#commitChkpt(java.lang.String)
	 */
	public final void commitChkpt(String filename) throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].commitChkpt(filename);
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#recover(java.lang.String)
	 */
	public final void recover(String filename) throws IOException {
		for (int i = 0; i < this.sets.length; i++) {
			this.sets[i].recover(filename);
		}
	}
}
