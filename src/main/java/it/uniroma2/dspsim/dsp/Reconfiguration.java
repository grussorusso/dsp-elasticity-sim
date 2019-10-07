package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.infrastructure.NodeType;

public class Reconfiguration {

	private NodeType[] instancesToAdd = null;
	private NodeType[] instancesToRemove = null;

	private Reconfiguration (NodeType[] toAdd, NodeType[] toRemove) {
		this.instancesToAdd = toAdd;
		this.instancesToRemove = toRemove;
	}

	static public Reconfiguration scaleOut (NodeType nt) {
		NodeType toAdd[] = { nt };
		return new Reconfiguration(toAdd, null);
	}

	static public Reconfiguration scaleIn (NodeType nt) {
		NodeType toRemove[] = { nt };
		return new Reconfiguration(null, toRemove);
	}

	static public Reconfiguration migration (NodeType from, NodeType to) {
		NodeType toAdd[] = { to };
		NodeType toRemove[] = { from };
		return new Reconfiguration(toAdd, toRemove);
	}

	public boolean isReconfiguration()
	{
		return (instancesToAdd != null && instancesToAdd.length > 0) || (instancesToRemove != null && instancesToRemove.length > 0);
	}

	// TODO multiple scaling

	static public Reconfiguration doNothing () {
		return new Reconfiguration(null, null);
	}

	public NodeType[] getInstancesToAdd() {
		return instancesToAdd;
	}

	public NodeType[] getInstancesToRemove() {
		return instancesToRemove;
	}

	@Override
	public String toString() {
		if (!isReconfiguration())
			return "(do nothing)";

		StringBuilder sb = new StringBuilder();

		if (instancesToAdd != null) {
			for (NodeType nt : instancesToAdd) {
				sb.append('+');
				sb.append(nt.getName());
				sb.append(' ');
			}
		}

		if (instancesToRemove != null) {
			for (NodeType nt : instancesToRemove) {
				sb.append('-');
				sb.append(nt.getName());
				sb.append(' ');
			}
		}

		return sb.toString();
	}
}
