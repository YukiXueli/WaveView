import java.util.*;

class TraceDataModel
{
	public NetTreeModel getNetTree()
	{
		return fNetTree;
	}
	
	TraceBuilder startBuilding()
	{
		fAllNets.clear();
		fFullNameToNetMap.clear();
		fNetTree.clear();

		return new ConcreteTraceBuilder();
	}

	AbstractTransitionIterator findTransition(int netId, long timestamp)
	{
		return fAllNets.elementAt(netId).findTransition(timestamp);
	}
	
	public long getMaxTimestamp()
	{
		return fMaxTimestamp;
	}

	public int getNetFromTreeObject(Object o)
	{
		return fNetTree.getNetFromTreeObject(o);
	}

	public int getTotalNetCount()
	{
		return fAllNets.size();
	}
	
	/// look up by fully qualified name
	public int findNet(String name)
	{
		Integer i = fFullNameToNetMap.get(name);
		if (i == null)
			return -1;
			
		return i.intValue();
	}

	public int getNetWidth(int index)
	{
		return fAllNets.elementAt(index).getWidth();
	}
	
	public String getShortNetName(int index)
	{
		return fAllNets.elementAt(index).getShortName();
	}

	public String getFullNetName(int index)
	{
		return fAllNets.elementAt(index).getFullName();
	}

	private class NetDataModel
	{
		NetDataModel(String shortName, int width)
		{
			fShortName = shortName;
			fTransitionVector = new TransitionVector(width);
		}
	
		// This NetDataModel shares its transition data with another one.
		/// @todo clean this up by separating into another class
		NetDataModel(String shortName, NetDataModel cloneFrom)
		{
			fShortName = shortName;
			fTransitionVector = cloneFrom.fTransitionVector;
		}
	
		void setFullName(String name)
		{
			fFullName = name;
		}
		
		String getFullName()
		{
			return fFullName;
		}

		String getShortName()
		{
			return fShortName;
		}
	
		AbstractTransitionIterator findTransition(long timestamp)
		{
			return fTransitionVector.findTransition(timestamp);
		}
	
		long getMaxTimestamp()
		{
			return fTransitionVector.getMaxTimestamp();
		}
	
		int getWidth()
		{
			return fTransitionVector.getWidth();
		}
	
		private TransitionVector fTransitionVector;
		private String fShortName;
		private String fFullName;
	}

	private class ConcreteTraceBuilder implements TraceBuilder
	{
		public void enterModule(String name)
		{
			fNetTree.enterScope(name);
			fScopeStack.push(name);
		}
	
		public void exitModule()
		{
			fNetTree.leaveScope();
			fScopeStack.pop();
		}

		public void loadFinished()
		{
			fMaxTimestamp = 0;
			for (NetDataModel model : fAllNets)
				fMaxTimestamp = Math.max(fMaxTimestamp, model.getMaxTimestamp());
		}

		public void appendTransition(int id, long timestamp, BitVector values)
		{
			NetDataModel model = fAllNets.elementAt(id);
			model.fTransitionVector.appendTransition(timestamp, values);
		}

		public int newNet(String shortName, int cloneId, int width)
		{
			// Build full path
			StringBuffer fullName = new StringBuffer();
			for (String scope : fScopeStack)
			{
				if (fullName.length() != 0)
					fullName.append('.');
					
				fullName.append(scope);
			}
	
			fullName.append('.');
			fullName.append(shortName);

			NetDataModel net;
			if (cloneId != -1)
				net = new NetDataModel(shortName, fAllNets.elementAt(cloneId));
			else
				net = new NetDataModel(shortName, width);

			net.setFullName(fullName.toString());
			fAllNets.addElement(net);
			int thisNetIndex = fAllNets.size() - 1;
			fNetTree.addNet(shortName, thisNetIndex);
			fFullNameToNetMap.put(fullName.toString(), thisNetIndex);
			return thisNetIndex;
		}
		
		public int getNetWidth(int netId)
		{
			return fAllNets.elementAt(netId).getWidth();
		}
		
		private Stack<String> fScopeStack = new Stack<String>();
	}

	private long fMaxTimestamp;
	private HashMap<String, Integer> fFullNameToNetMap = new HashMap<String, Integer>();
	private Vector<NetDataModel> fAllNets = new Vector<NetDataModel>();
	private NetTreeModel fNetTree = new NetTreeModel();
}
