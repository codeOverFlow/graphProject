import java.io.*;
import java.util.*;

/**
 * @author Adrien Bodineau, Clement Bauchet
 * @version 0.1
 */
class Appli {
	/**
	 * a map used to find circuits
	 */
	private static final Map<String, ArrayList<String>> b = new TreeMap<>();

	/**
	 * a map used to find circuits by storing blocked vertices
	 */
	private static final Map<String, Boolean> blocked = new TreeMap<>();

	/**
	 * stack used to find circuits
	 */
	private static final Stack<String> stack = new Stack<>();

	/**
	 * the graph structure
	 */
	private static Map<String, ArrayList<String>> graph;

	/**
	 * save the founded circuits
	 */
	private static ArrayList<ArrayList<String>> save_circuits;


	/********** CONSTRUCTOR & METHODS **********/


	/**
	 * @param args program arguments
	 */
	public static void main(String[] args) {
		// variables
		int k;
		String seq;

		// read the data file
		try {
			InputStream ips = new FileInputStream(args[0]);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);

			// read k, the size of k-mer
			k = Integer.parseInt(br.readLine());
			System.out.println("k = " + k + "\n");
			// process all line
			while ((seq = br.readLine()) != null) {
				System.out.println(seq);
				for (int i = 1; i < seq.length() / 2; i++) {
					k = i;
					build_graph(k, seq);
					find_circuits(k);
					search_tandem(k, seq);
					find_non_trivial(k, seq);
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * build the Bruijn graph from the string S, with k-mer of size k
	 *
	 * @param k size of k-mer
	 * @param S the string to analyze
	 */
	private static void build_graph(int k, String S) {
		// variables
		graph = new TreeMap<>();
		save_circuits = new ArrayList<>();
		// create all vertices
		for (int i = 0; i < S.length() - k; i++) {
			graph.put(S.substring(i, i + k), new ArrayList<String>());
		}
		// find all children of each vertices
		for (String x : graph.keySet()) {
			String s = x.substring(1, x.length());
			for (String y : graph.keySet()) {
				// check if y is a child of x
				if (s.equals(y.substring(0, y.length() - 1))) {
					String tmp = x + y.charAt(y.length() - 1);
					for (int i = 0; i < S.length() - k; i++) {
						if (S.substring(i, i + k + 1).equals(tmp)) {
							graph.get(x).add(y);
							break;
						}
					}
				}
			}
		}
		// save the graph in a file
		save_graph(S, k);
	}

	/**
	 * initialize variables to find circuits
	 *
	 * @param k the size of k-mer
	 */
	private static void find_circuits(int k) {
		// clear the stack
		stack.clear();
		// get the set of vertices
		SortedSet<String> tmp = new TreeSet<>((SortedSet<String>) graph.keySet());
		String s;

		// number of iteration
		int n = 0;
		int max = tmp.size();
		// while all vertices have not be analyzed
		while (n < max) {
			// take the first vertex
			s = tmp.first();
			// for each vertices set bloked(v) = false
			for (String i : tmp) {
				blocked.put(i, false);
				b.put(i, new ArrayList<String>());
			}
			// find a circuit from s
			circuit(s, s, k);
			n++;
			// remove s from the set
			tmp.remove(s);
		}
	}

	/**
	 * found a circuit from a vertex
	 *
	 * @param v the current vertex
	 * @param s the starting vertex
	 * @param k the size of k-mer
	 * @return true if there is a circuit for s, false otherwise
	 */
	private static boolean circuit(String v, String s, int k) {
		// set the ending boolean to false
		boolean f = false;
		// push v in the stack
		stack.push(v);
		// set blocked(v) = true
		blocked.put(v, true);
		// if a child of v is s
		if (graph.get(v).contains(s)) {
			// store the circuit in an ArrayList
			ArrayList<String> arr = new ArrayList<>();
			for (String e : stack) {
				arr.add(e);
			}

			// if the size of the circuit is less or equals to k
			// store it in save_circuits
			if (arr.size() <= k) {
				save_circuits.add(arr);
			}
			// set the ending boolean to true
			f = true;
		}
		else {
			// for each non blocked children of v, continue to find a circuit
			for (String w : graph.get(v)) {
				if (!blocked.get(w)) {
					if (circuit(w, s, k))
						f = true;
				}
			}
		}
		// if we found a circuit, unblock v
		if (f)
			unblock(v);
		else {
			if (graph.containsKey(v)) {
				for (String w : graph.get(v))
					if (!b.get(w).contains(v))
						b.get(w).add(v);
			}
		}
		// pop the stack
		stack.pop();
		// return the ending boolean
		return f;
	}

	/**
	 * unblock a blocked vertex
	 *
	 * @param u the vertex to unblock
	 */
	private static void unblock(String u) {
		// unblock u
		blocked.put(u, false);
		while (b.get(u).size() > 0) {
			String x = b.get(u).remove(0);
			if (blocked.get(x))
				unblock(x);
		}
	}

	/**
	 * save the Bruijn graph in a dot file format into the dot/ directory
	 * and the matching svg file in svg/ directory
	 *
	 * @param S the string used to built the graph (used to name the files)
	 */
	private static void save_graph(String S, int k) {
		// create a new file in dot/
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter("./dot/" + S + "_" + k)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		assert pw != null;

		// print the graph in dot file format
		pw.println("digraph Bruijn {");
		for (String x : graph.keySet()) {
			pw.println(x + "[label=" + x + "]");
			for (String y : graph.get(x)) {
				pw.println(x + "->" + y);
				pw.println(y + "[label=" + y + "]");
			}
		}
		pw.println("}");
		pw.close();

		// create the svg file from the dot file
		try {
			Process p = Runtime.getRuntime().exec("dot -O -Tsvg ./dot/" + S + "_" + k);
			p.waitFor();
			p = Runtime.getRuntime().exec("mv ./dot/" + S + "_" + k + ".svg ./svg");
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * save the tandem repeat in a txt file format into the tandem/ directory
	 *
	 * @param S          the string used to find tandem repeats (used to name the file)
	 * @param arrayLists matrix containing information on each tandem repeats
	 */
	private static void save_tandem(String S, ArrayList<ArrayList<Object>> arrayLists, int k) {
		// create a new file in tandem/
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter("./tandem/" + S, true)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		assert pw != null;
		// print all tandem repeats in the file
		pw.print("****************** REPETITION EN TANDEM DE TAILLE " + k + " ********************\n");
		for (ArrayList<Object> a : arrayLists) {
			pw.println(a.get(0) + ": {debut: " + a.get(1) + ", fin: " + a.get(2) + "}\n");
		}
		pw.close();
	}

	private static void save_non_trivial(String S, ArrayList<String> arrayLists, int k) {
		// create a new file in tandem/
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter("./nonTrivial/" + S, true)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		assert pw != null;
		// print all tandem repeats in the file
		pw.print("****************** REPETITION NON TRIVIAL DE TAILLE " + k + " ********************\n");
		for (String a : arrayLists) {
			pw.println(a + "\n");
		}
		pw.close();
	}

	/**
	 * search all tandem repeats
	 *
	 * @param k   size of the k-mer
	 * @param seq the string used to find tandem repeats
	 * @return a matrix containing informations on tandem repeats
	 */
	private static ArrayList<ArrayList<Object>> search_tandem(int k, String seq) {
		ArrayList<ArrayList<Object>> tandem = new ArrayList<>();
		for (ArrayList<String> a : save_circuits) {
			ArrayList<Integer> index = new ArrayList<>();
			String s = a.get(0);
			index.add(seq.indexOf(s, 0));
			int ii;
			while ((ii = seq.indexOf(s, index.get(index.size() - 1) + k)) != -1) {
				index.add(ii);
			}

			//tandem = new ArrayList<>();
			ArrayList<ArrayList<Integer>> find_tandem = new ArrayList<>();
			ArrayList<Integer> tmp = new ArrayList<>();
			tmp.add(index.get(0));
			for (int i = 1; i < index.size(); i++) {
				if (index.get(i) - tmp.get(tmp.size() - 1) != k) {
					if (tmp.size() >= 2)
						find_tandem.add(tmp);
					tmp = new ArrayList<>();
					tmp.add(index.get(i));
				}
				else {
					tmp.add(index.get(i));
				}
			}
			if (tmp.size() >= 2)
				find_tandem.add(tmp);

			for (ArrayList<Integer> arr : find_tandem) {
				ArrayList<Object> tab = new ArrayList<>();
				tab.add(s);
				tab.add(arr.get(0));
				tab.add(arr.get(arr.size() - 1) + k - 1);
				tandem.add(tab);
			}
		}
		if (tandem.size() > 0)
			save_tandem(seq + ".txt", tandem, k);
		return tandem;
	}

	/**
	 * find all non trivial repeats
	 *
	 * @param k   size of the k-mer
	 * @param seq the string used to find non trivial repeats
	 * @return an ArrayList containing all k-mer that are non trivial repeats
	 */
	private static ArrayList<String> find_non_trivial(int k, String seq) {
		// create an ArrayList containing all k+1-mer
		ArrayList<String> arr = new ArrayList<>();
		for (int i = 0; i < seq.length() - k - 1; i++) {
			arr.add(seq.substring(i, i + k + 1));
		}

		ArrayList<String> non_trivial = new ArrayList<>();
		// for each k-mer check if it is a non trivial repeat or not
		for (String kmer : graph.keySet()) {
			// find the number of repetition of the k-mer
			ArrayList<Integer> tmp = new ArrayList<>();
			int index = -1;
			while ((index = seq.indexOf(kmer, index + 1)) != -1) {
				tmp.add(index);
			}

			if (tmp.size() >= 2) {
				// contains all k+1-mer of the format: k-mer+u
				ArrayList<String> in = new ArrayList<>();

				// contains all k+1-mer of the format u+k-mer
				ArrayList<String> in2 = new ArrayList<>();

				// fill in and in2 ArrayList
				for (String kpumer : arr) {
					if (kpumer.substring(0, k).equals(kmer)) {
						in.add(kpumer);
					}
					else if (kpumer.substring(1, kpumer.length()).equals(kmer)) {
						in2.add(kpumer);
					}
				}

				// if both ArrayList size are less than the number of repeats
				// so it is a non trivial repeats
				if (in.size() != tmp.size() && in2.size() != tmp.size()) {
					non_trivial.add(kmer);
				}
				else {
					// otherwise check if the k-mer is surrounded
					// by the same pattern or not
					boolean surrounded = true;
					for (int i = 0; i < in.size() - 1; i++) {
						if (!in.get(i).equals(in.get(i + 1))) {
							surrounded = false;
							break;
						}
					}
					boolean surrounded2 = true;
					for (int i = 0; i < in2.size() - 1; i++) {
						if (!in2.get(i).equals(in2.get(i + 1))) {
							surrounded2 = false;
							break;
						}
					}
					// if patterns are not the same
					// so it is a non trivial repeats
					if (!surrounded && !surrounded2) {
						non_trivial.add(kmer);
					}
				}
			}
		}
		if (non_trivial.size() > 0)
			save_non_trivial(seq + ".txt", non_trivial, k);
		// return the ArrayList containing all non trivial repeats
		return non_trivial;
	}
}
