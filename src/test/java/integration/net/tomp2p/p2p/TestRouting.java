package net.tomp2p.p2p;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.tomp2p.Utils2;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureForkJoin;
import net.tomp2p.futures.FutureResponse;
import net.tomp2p.futures.FutureRouting;
import net.tomp2p.message.Message.Command;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.Routing;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerMapKadImpl;
import net.tomp2p.utils.Utils;

import org.junit.Assert;
import org.junit.Test;

public class TestRouting
{
	final private static Random rnd = new Random(43L);

	@Test
	public void testDifference() throws UnknownHostException
	{
		PeerMapKadImpl test = new PeerMapKadImpl(new Number160(77), 2, 100, 60 * 1000, 3,
				new int[] {});
		Collection<PeerAddress> newC = new ArrayList<PeerAddress>();
		newC.add(Utils2.createAddress(12));
		newC.add(Utils2.createAddress(15));
		newC.add(Utils2.createAddress(88));
		newC.add(Utils2.createAddress(90));
		newC.add(Utils2.createAddress(91));
		SortedSet<PeerAddress> result = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		SortedSet<PeerAddress> already = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		already.add(Utils2.createAddress(90));
		already.add(Utils2.createAddress(15));
		Utils.difference(newC, already, result);
		Assert.assertEquals(3, result.size());
		Assert.assertEquals(Utils2.createAddress(88), result.first());
	}

	@Test
	public void testMerge() throws UnknownHostException
	{
		PeerMapKadImpl test = new PeerMapKadImpl(new Number160(77), 2, 100, 60 * 1000, 3,
				new int[] {});
		SortedSet<PeerAddress> queue = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		SortedSet<PeerAddress> neighbors = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		SortedSet<PeerAddress> already = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		queue.add(Utils2.createAddress(12));
		queue.add(Utils2.createAddress(14));
		queue.add(Utils2.createAddress(16));
		//
		neighbors.add(Utils2.createAddress(88));
		neighbors.add(Utils2.createAddress(12));
		neighbors.add(Utils2.createAddress(16));
		//
		already.add(Utils2.createAddress(16));
		boolean testb = Routing.merge(queue, neighbors, already);
		Assert.assertEquals(true, testb);
		// next one
		neighbors.add(Utils2.createAddress(89));
		testb = Routing.merge(queue, neighbors, already);
		Assert.assertEquals(false, testb);
		// next one
		neighbors.add(Utils2.createAddress(88));
		testb = Routing.merge(queue, neighbors, already);
		Assert.assertEquals(false, testb);
	}

	@Test
	public void testEvaluate() throws UnknownHostException
	{
		PeerMapKadImpl test = new PeerMapKadImpl(new Number160(77), 2, 100, 60 * 1000, 3,
				new int[] {});
		SortedSet<PeerAddress> queue = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		SortedSet<PeerAddress> neighbors = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		SortedSet<PeerAddress> already = new TreeSet<PeerAddress>(test
				.createPeerComparator(new Number160(88)));
		//
		queue.add(Utils2.createAddress(12));
		queue.add(Utils2.createAddress(14));
		queue.add(Utils2.createAddress(16));
		//
		neighbors.add(Utils2.createAddress(89));
		neighbors.add(Utils2.createAddress(12));
		neighbors.add(Utils2.createAddress(16));
		//
		already.add(Utils2.createAddress(16));
		//
		AtomicInteger nrNoNewInformation = new AtomicInteger();
		boolean testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation,
				0);
		Assert.assertEquals(0, nrNoNewInformation.get());
		Assert.assertEquals(false, testb);
		testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation, 2);
		Assert.assertEquals(1, nrNoNewInformation.get());
		Assert.assertEquals(false, testb);
		neighbors.add(Utils2.createAddress(11));
		testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation, 2);
		Assert.assertEquals(2, nrNoNewInformation.get());
		Assert.assertEquals(true, testb);
		neighbors.add(Utils2.createAddress(88));
		testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation, 2);
		Assert.assertEquals(0, nrNoNewInformation.get());
		Assert.assertEquals(false, testb);
		//
		testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation, 2);
		Assert.assertEquals(1, nrNoNewInformation.get());
		neighbors.add(Utils2.createAddress(89));
		testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation, 2);
		Assert.assertEquals(2, nrNoNewInformation.get());
		neighbors.add(Utils2.createAddress(88));
		testb = Routing.evaluateInformation(neighbors, queue, already, nrNoNewInformation, 2);
		Assert.assertEquals(3, nrNoNewInformation.get());
		Assert.assertEquals(true, testb);
	}

	@Test
	public void testRouting1TCP() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting2TCP() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting1UDP() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting2UDP() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting2() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(), Utils2
					.createAddress("0xffffff"));
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			// node5 cannot be reached, so it should not be part of the result
			Assert.assertEquals(false, nodes[5].getPeerAddress().equals(ns.first()));
			Assert.assertEquals(true, nodes[4].getPeerAddress().equals(ns.first()));
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting2_detailed() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(), Utils2
					.createAddress("0xffffff"));
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			// node5 cannot be reached, so it should not be part of the result
			Assert.assertEquals(false, nodes[5].getPeerAddress().equals(ns.first()));
			Assert.assertEquals(true, nodes[4].getPeerAddress().equals(ns.first()));
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting3() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[5], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 1, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			// 
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
			Assert.assertEquals(false, ns.contains(nodes[3].getPeerAddress()));
			Assert.assertEquals(false, ns.contains(nodes[4].getPeerAddress()));
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting3_detailed() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[5], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 1, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			// 
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
			Assert.assertEquals(false, ns.contains(nodes[3].getPeerAddress()));
			Assert.assertEquals(false, ns.contains(nodes[4].getPeerAddress()));
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting4() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[5], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 2, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			// 
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
			Assert.assertEquals(true, ns.contains(nodes[0].getPeerAddress()));
			Assert.assertEquals(true, ns.contains(nodes[1].getPeerAddress()));
			Assert.assertEquals(true, ns.contains(nodes[2].getPeerAddress()));
			Assert.assertEquals(false, ns.contains(nodes[3].getPeerAddress()));
			Assert.assertEquals(true, ns.contains(nodes[4].getPeerAddress()));
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting5() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[5], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 3, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			// 
			Assert.assertEquals(nodes[5].getPeerAddress(), ns.first());
			Assert.assertEquals(true, ns.contains(nodes[3].getPeerAddress()));
			Assert.assertEquals(true, ns.contains(nodes[4].getPeerAddress()));
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	@Test
	public void testRouting6() throws Exception
	{
		Peer[] nodes = null;
		try
		{
			nodes = createNodes(7);
			//
			addToMap(nodes[0], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[1], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress());
			addToMap(nodes[2], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress(), nodes[2]
					.getPeerAddress(), nodes[3].getPeerAddress(), nodes[4].getPeerAddress(),
					nodes[5].getPeerAddress());
			addToMap(nodes[3], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[4], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			addToMap(nodes[5], true, nodes[0].getPeerAddress(), nodes[1].getPeerAddress());
			FutureRouting fr = nodes[0].getRouting().route(nodes[6].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 3, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			System.err.println(ns.size());
			Assert.assertEquals(6, ns.size());
		}
		finally
		{
			for (Peer n : nodes)
				n.shutdown();
		}
	}

	private void addToMap(Peer n1, boolean marker, PeerAddress... p)
	{
		for (int i = 0; i < p.length; i++)
		{
			n1.getPeerBean().getPeerMap().peerOnline(p[i], null);
		}
	}

	private Peer[] createNodes(int nr) throws Exception
	{
		StringBuilder sb = new StringBuilder("0x");
		Peer[] nodes = new Peer[nr];
		for (int i = 0; i < nr; i++)
		{
			sb.append("f");
			nodes[i] = new Peer(new Number160(sb.toString()));
			nodes[i].listen(4001 + i, 4001 + i);
		}
		return nodes;
	}

	@Test
	public void testRoutingBulkTCP() throws Exception
	{
		Peer master = null;
		try
		{
			master = new Peer(new Number160(rnd));
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 2000);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			FutureRouting fr = nodes[500].getRouting().route(nodes[20].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 1, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			Assert.assertEquals(nodes[20].getPeerAddress(), ns.first());
		}
		finally
		{
			master.shutdown();
		}
	}

	@Test
	public void testRoutingBulkUDP() throws Exception
	{
		Peer master = null;
		try
		{
			master = new Peer(new Number160(rnd));
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 2000);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			FutureRouting fr = nodes[500].getRouting().route(nodes[20].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 0, 0, 1, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			Assert.assertEquals(nodes[20].getPeerAddress(), ns.first());
		}
		finally
		{
			master.shutdown();
		}
	}

	private Peer[] createNodes(Peer master, int nr) throws Exception
	{
		Peer[] nodes = new Peer[nr];
		for (int i = 0; i < nr; i++)
		{
			nodes[i] = new Peer(new Number160(rnd));
			nodes[i].listen(master);
		}
		return nodes;
	}

	@Test
	public void testRoutingConcurrentlyTCP() throws Exception
	{
		Peer master = null;
		try
		{
			master = new Peer(new Number160(rnd));
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 2000);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			System.err.println("do rounting");
			List<FutureRouting> frs = new ArrayList<FutureRouting>();
			for (int i = 0; i < nodes.length; i++)
			{
				FutureRouting frr = nodes[((i * 7777) + 1) % nodes.length].getRouting().route(
						nodes[((i * 3333) + 1) % nodes.length].getPeerID(), null, null,
						Command.NEIGHBORS_STORAGE, 0, 0, 0, 1, true);
				frs.add(frr);
				// slow down or we have a too many open files in system
			}
			System.err.println("run Forrest, run!");
			for (int i = 0; i < nodes.length; i++)
			{
				frs.get(i).awaitUninterruptibly();
				Assert.assertEquals(true, frs.get(i).isSuccess());
				SortedSet<PeerAddress> ns = frs.get(i).getPotentialHits();
				Assert.assertEquals(nodes[((i * 3333) + 1) % nodes.length].getPeerAddress(), ns
						.first());
			}
			System.err.println("done!");
		}
		finally
		{
			master.shutdown();
		}
	}

	@Test
	public void testRoutingConcurrentlyTCP2() throws Exception
	{
		Peer master = null;
		try
		{
			master = new Peer(new Number160(rnd));
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 2000);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			Map<Integer, FutureRouting> frs = new HashMap<Integer, FutureRouting>();
			for (int i = 0; i < nodes.length; i++)
			{
				FutureRouting frr = nodes[((i * 7777) + 1) % nodes.length].getRouting().route(
						nodes[((i * 3333) + 1) % nodes.length].getPeerID(), null, null,
						Command.NEIGHBORS_STORAGE, 0, 5, 0, 2, true);
				frs.put(i, frr);
			}
			System.err.println("run Forrest, run!");
			for (int i = 0; i < nodes.length; i++)
			{
				System.err.println(i);
				frs.get(i).awaitUninterruptibly();
				Assert.assertEquals(true, frs.get(i).isSuccess());
				SortedSet<PeerAddress> ns = frs.get(i).getPotentialHits();
				Assert.assertEquals(nodes[((i * 3333) + 1) % nodes.length].getPeerAddress(), ns
						.first());
			}
			System.err.println("done!");
		}
		finally
		{
			master.shutdown();
		}
	}

	@Test
	public void testRoutingConcurrentlyUDP() throws Exception
	{
		Peer master = null;
		try
		{
			master = new Peer(new Number160(rnd));
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 2000);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			List<FutureRouting> frs = new ArrayList<FutureRouting>();
			for (int i = 0; i < nodes.length; i++)
			{
				FutureRouting frr = nodes[((i * 7777) + 1) % nodes.length].getRouting().route(
						nodes[((i * 3333) + 1) % nodes.length].getPeerID(), null, null,
						Command.NEIGHBORS_STORAGE, 0, 0, 0, 1, false);
				frs.add(frr);
			}
			System.err.println("run Forrest, run!");
			for (int i = 0; i < nodes.length; i++)
			{
				frs.get(i).awaitUninterruptibly();
				Assert.assertEquals(true, frs.get(i).isSuccess());
				SortedSet<PeerAddress> ns = frs.get(i).getPotentialHits();
				Assert.assertEquals(nodes[((i * 3333) + 1) % nodes.length].getPeerAddress(), ns
						.first());
			}
			System.err.println("done!");
		}
		finally
		{
			master.shutdown();
		}
	}

	@Test
	public void testRoutingConcurrentlyUDP2() throws Exception
	{
		Peer master = null;
		try
		{
			master = new Peer(new Number160(rnd));
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 2000);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			List<FutureRouting> frs = new ArrayList<FutureRouting>();
			for (int i = 0; i < nodes.length; i++)
			{
				FutureRouting frr = nodes[((i * 7777) + 1) % nodes.length].getRouting().route(
						nodes[((i * 3333) + 1) % nodes.length].getPeerID(), null, null,
						Command.NEIGHBORS_STORAGE, 0, 1, 0, 2, false);
				frs.add(frr);
			}
			System.err.println("run Forrest, run!");
			for (int i = 0; i < nodes.length; i++)
			{
				frs.get(i).awaitUninterruptibly();
				Assert.assertEquals(true, frs.get(i).isSuccess());
				SortedSet<PeerAddress> ns = frs.get(i).getPotentialHits();
				Assert.assertEquals(nodes[((i * 3333) + 1) % nodes.length].getPeerAddress(), ns
						.first());
			}
			System.err.println("done!");
		}
		finally
		{
			master.shutdown();
		}
	}

	@Test
	public void testRoutingFailures() throws Exception
	{
		Peer master = new Peer(new Number160(rnd));
		try
		{
			master.listen(4001, 4001);
			int len = 2000;
			Peer[] nodes = createNodes(master, len);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			for (int i = 0; i < 2000; i++)
			{
				if (i != 20 && i != 500)
				{
					System.out.println("Shutting down "
							+ nodes[i].getPeerBean().getServerPeerAddress().getID());
					nodes[i].shutdown();
				}
			}
			FutureRouting fr = nodes[500].getRouting().route(nodes[20].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 1, 1, 1, false);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			if (ns.size() > 0)
				Assert.assertEquals(nodes[20].getPeerAddress(), ns.first());
			else
				Assert.fail("nothing returned");
		}
		finally
		{
			System.err.println("almost done");
			master.shutdown();
		}
	}

	@Test
	public void testRoutingFailuresTCP() throws Exception
	{
		Peer master = new Peer(new Number160(rnd));
		try
		{
			master.listen(4001, 4001);
			int len = 2000;
			Peer[] nodes = createNodes(master, len);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			for (int i = 0; i < 2000; i++)
			{
				if (i != 20 && i != 500)
				{
					System.out.println("Shutting down "
							+ nodes[i].getPeerBean().getServerPeerAddress().getID());
					nodes[i].shutdown();
				}
			}
			FutureRouting fr = nodes[500].getRouting().route(nodes[20].getPeerID(), null, null,
					Command.NEIGHBORS_STORAGE, 0, 1, 1, 1, true);
			fr.awaitUninterruptibly();
			Assert.assertEquals(true, fr.isSuccess());
			SortedSet<PeerAddress> ns = fr.getPotentialHits();
			if (ns.size() > 0)
				Assert.assertEquals(nodes[20].getPeerAddress(), ns.first());
			else
				Assert.fail("nothing returned");
		}
		finally
		{
			System.err.println("almost done");
			master.shutdown();
		}
	}

	@Test
	public void testRoutingBootstrap1() throws Exception
	{
		Peer master = new Peer(new Number160(rnd));
		try
		{
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 100);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			for (int i = 1; i < nodes.length; i++)
			{
				Collection<PeerAddress> peerAddresses = new ArrayList<PeerAddress>(1);
				peerAddresses.add(nodes[0].getPeerAddress());
				FutureBootstrap fm = nodes[i].getRouting()
						.bootstrap(peerAddresses, 5, 100, 1, true);
				fm.awaitUninterruptibly();
				Assert.assertEquals(true, fm.isSuccess());
			}
		}
		finally
		{
			master.shutdown();
		}
	}

	@Test
	public void testRoutingBootstrap2() throws Exception
	{
		Peer master = new Peer(new Number160(rnd));
		try
		{
			master.listen(4001, 4001);
			Peer[] nodes = createNodes(master, 100);
			for (int i = 0; i < nodes.length; i++)
			{
				for (int j = 0; j < nodes.length; j++)
					nodes[i].getPeerBean().getPeerMap().peerOnline(nodes[j].getPeerAddress(), null);
			}
			for (int i = 1; i < nodes.length; i++)
			{
				Collection<PeerAddress> peerAddresses = new ArrayList<PeerAddress>(1);
				peerAddresses.add(nodes[0].getPeerAddress());
				FutureBootstrap fm = nodes[i].getRouting().bootstrap(peerAddresses, 5, 100, 1,
						false);
				fm.awaitUninterruptibly();
				Assert.assertEquals(true, fm.isSuccess());
			}
		}
		finally
		{
			master.shutdown();
		}
	}

	// works only in 1.6
	@Test
	public void testBootstrap() throws Exception
	{
		Peer master = new Peer(new Number160(rnd));
		Peer client = new Peer(new Number160(rnd));
		try
		{
			master.listen();
			client.listen(4001, 4001);
			FutureForkJoin<FutureResponse> tmp = client.pingBroadcast();
			tmp.awaitUninterruptibly();
			Assert.assertEquals(true, tmp.isSuccess());
			Assert.assertEquals(1, client.getPeerBean().getPeerMap().size());
		}
		finally
		{
			client.shutdown();
			master.shutdown();
		}
	}

	// works only in 1.6
	@Test
	public void testBootstrap2() throws Exception
	{
		Peer master = new Peer(new Number160(rnd));
		Peer client = new Peer(new Number160(rnd));
		try
		{
			master.listen(4002, 4002);
			client.listen(4001, 4001);
			FutureForkJoin<FutureResponse> tmp = client.pingBroadcast();
			tmp.awaitUninterruptibly();
			Assert.assertEquals(false, tmp.isSuccess());
			Assert.assertEquals(0, client.getPeerBean().getPeerMap().size());
		}
		finally
		{
			client.shutdown();
			master.shutdown();
		}
	}
}