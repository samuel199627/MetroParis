package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

//IL MAIN E' QUI

//Questo progetto serve solo per farci capire come creare un grafo partendo da un database.

/*
 	Le linee sono tutti i percorsi della metro che quindi si diramano e su cui giaciono le nostre stazioni  e di conseguenza gli archi.
 	
 	Nella mappa immagine che abbiamo in input all'esercizio, quello che abbiamo e' un multigrafo (piu' archi nella stessa direzione
 	che collegano stesse stazioni, cioe' abbiamo archi multipli).
 	
 	Per la maggior parte il grafo e' non orientato, ma una zona ha un ciclo orientato e dunque dobbiamo crearci il grafo in maniera 
 	orientata, pero' evitiamo il multigrafo e teniamo un'unica connessione (quindi grafo semplice).
 	
 	Quello che creiamo e' un grafo semplice, orientato e non pesato in quanto sulla mappa e nel database non abbiamo informazioni
 	sui pesi delle connessioni.
 	
 	Connessione collega le stazioni e in lei qui in java salviamo gli oggetti a differenza dei soli id che sono presenti in SQL anche
 	se poi e' una classe che per la costruzione del grafo non ci serve, ma che per completezza e' bene far vedere come salvare per 
 	avere un riscontro con quanto visto a teoria.
 	
 	Fermata e' una tabella, mentre nella tabella Connessione figura come Stazione.
 	
 	
 */



public class Model {
	
	//defaultEdge perche' non e' pesato
	//noi lo vogliamo semplice (non importiamo archi multipli), non pesato ed orientato(una zona della cartina ha gli archi orientati)
	private Graph<Fermata, DefaultEdge> graph ;
	private List<Fermata> fermate ;
	private Map<Integer, Fermata> fermateIdMap ;
	
	public Model() {
		//classe corrispondente al grafo che vogliamo crearci
		this.graph = new SimpleDirectedGraph<>(DefaultEdge.class) ;
		
		MetroDAO dao = new MetroDAO() ;
		
		// CREAZIONE DEI VERTICI del grafo che sono le nostre fermate
		this.fermate = dao.getAllFermate() ;
		
		//ci creiamo la mappa di fermate perche' potrebbe servire (non e' cosi' necessaria per il grafo in se')
		//ci serve quando interroghiamo la tabella Connessione che ci restituisce l'id della fermata, ma noi
		//associamo direttamente l'oggetto a questo id in modo da avere le informazioni molto piu' complete.
		this.fermateIdMap = new HashMap<>() ;
		for(Fermata f: this.fermate) {
			fermateIdMap.put(f.getIdFermata(), f) ;
		}
		
		//aggiungiamo tutti i veritici
		Graphs.addAllVertices(this.graph, this.fermate) ;
		
		//questa e' la stampa del grafo che stampa prima tutti i vertici e poi tutti gli archi
		//System.out.println(this.graph) ;
		
		// CREAZIONE DEGLI ARCHI -- metodo 1 (coppie di vertici)
		//per ogni coppia di vertici andiamo a chiedere al DAO se c'e' una connessione tra i veritici
		//e se si aggiungo l'arco corrispondente nel grafo
		/*
		//per ognuna delle fermate, scorro tutte le altre per vedere se c'e' una connessione tra le due
		//come ha fatto vedere nel video, ci mette un sacco di tempo a provare tutte queste query in quanto
		//andiamo a cercare tutte le soluzioni possibili
		for(Fermata fp : this.fermate) {
			for(Fermata fa : this.fermate) {
			//facciamo un'istruzione che ritorna true se c'e' una connessione tra le fermate che passiamo come 
			//parametro e in quel caso aggiungiamo l'arco
				if( dao.fermateConnesse(fp, fa) ) {
					this.graph.addEdge(fp, fa) ;
				}
			}
		}
		*/
		
		// CREAZIONE DEGLI ARCHI -- metodo 2 (da un vertice, trova tutti i connessi)
		//quindi invece che provare tutti i potenziali archi, cioe' immaginiamo un grafo completo e verifichiamo 
		//tutti i possibili archi che possono esserci.
		//Qui andiamo invece a prendere un vertice e ci facciamo dare con un'unica query  tutte le stazioni connesse
		//a questa di parametro e cosi' facendo sicuramente diminuiamo drasticamente il numero di query
		/*
		for(Fermata fp: this.fermate) {
			List<Fermata> connesse = dao.fermateSuccessive(fp, fermateIdMap) ;
			
			for(Fermata fa: connesse) {
				this.graph.addEdge(fp, fa) ;
			}
		}
		*/
		
		// CREAZIONE DEGLI ARCHI -- metodo 3 (chiedo al DB l'elenco degli archi)
		//interroghiamo connessione e ci restituisce direttamente tutte le connessione
		//leggendo in ordine la tabella e poi creiamo le connessioni direttamente dalle coppie
		//restituite
		List<CoppiaFermate> coppie = dao.coppieFermate(fermateIdMap) ;
		for(CoppiaFermate c : coppie) {
			this.graph.addEdge(c.getFp(), c.getFa()) ;
		}
				
		
//		System.out.println(this.graph) ;
		System.out.format("Grafo caricato con %d vertici %d archi",
				this.graph.vertexSet().size(),
				this.graph.edgeSet().size());

	}
	
	
	
	/**
	 * Visita l'intero grafo con la strategia Breadth First
	 * e ritorna l'insieme dei vertici incontrati.
	 * @param source vertice di partenza della visita
	 * @return insieme dei vertici incontrati
	 */
	public List<Fermata> visitaAmpiezza(Fermata source) {
		/*
		 	Partendo da una fermata restituisce una lista di fermate. 
		 	La visita in ampiezza e' che ad ogni passo visita tutti nodi alla stessa distanza (a livello di 
		 	numeri di archi che li separa) da quello iniziale e quindi si muove come su cerchi concentrici.
		 	L'oggetto iteratore per questo tipo di visita e' BreadthFirstIterator e ha bisogno del grafo e del
		 	vertice di partenza. Funziona come un puntatore e finche' c'e' un successivo continua a muoversi e
		 	ci salviamo in una lista in ordine i vertici che visita. Provando a stampare questa visita 
		 	non risulta molto chiaro perche' tutto viene stampato in sequenza e quindi dovremmo aiutarci con
		 	la cartina in ingresso delle fermate per capire bene i cerchi concentrici dove si separano.
		*/
		
		List<Fermata> visita = new ArrayList<>();
		
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source);
		while(bfv.hasNext()) {
			visita.add( bfv.next() ) ;
		}
		
		return visita ;
	}
	
	// ritorna una mappa del tipo:
	// <nuovo vertice scoperto, vertice da cui l'ho scoperto>
	//cioe' <figlio, padre> 
	/*
	 	Ci serve una classe anonima inline che implementa l'interfaccia TraversalListener per poter
	 	avere le informazioni dell'arco (in questo caso e' quello che serve a noi) che l'iteratore sta
	 	scorrendo. Ogni volta che l'iteratore viene fatto procedere di una posizione abbiamo in pratica
	 	questo metodo che viene richiamato e possiamo estrarre le informazioni sull'arco che abbiamo
	 	appena percorso per andare da un vertice al successivo. 
	 	Facciamo riferimento all'iteratore dell'ispezione in ampiezza,
	 */
	public Map<Fermata, Fermata> alberoVisita(Fermata source) {
		//final ci serve per permetterci di poter fare riferimento ad 'albero' anche dentro alla classe anonima
		//che ci siamo creati che implementa l'interfaccia TraversalListener.
		//Essendo l'albero di visita che ci creiamo un HashMap, andiamo a perdere l'ordine di inserimento di 
		//percorrenza degli altri e ci resteranno solo le percorrenze figlio<-padre
		final Map<Fermata,Fermata> albero = new HashMap<>();
		albero.put(source, null) ;
		
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source);
		
		bfv.addTraversalListener(new TraversalListener<Fermata, DefaultEdge>() {
			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {}
			
			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {}
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) {
				// la visita sta considerando un nuovo arco.
				// questo arco ha scoperto un nuovo vertice?
				// se sì, provenendo da dove?
				DefaultEdge edge = e.getEdge(); // (a,b) : ho scoperto 'a' partendo da 'b' oppure 'b' da 'a'
				//sorgente arco
				Fermata a = graph.getEdgeSource(edge);
				//destinazione arco
				Fermata b = graph.getEdgeTarget(edge);
				
				/*
				 	Per aggiungere l'arco che l'iteratore ha appena percorso devo controllare che la sorgente ci sia
				 	nell'albero di visita e che non ci sia ancora la destinazione in modo che riusciamo a mettere 
				 	la connessione padre figlio in questione.
				 	Quello che si vuole evitare e' di non mettere archi per nodi che erano gia' presenti nella visita
				 	e quindi per nodi che vanno poi cosi' a creare un ciclo. Stessa cosa non vogliamo creare visite
				 	per nodi che non sono ancora stati visitati (nessuno dei due).
				 	
				 	Essendo questo grafo che creiamo orientrato, penso che l'else if non ci vada in realta' perche' 
				 	qui gli archi sono orientati e quindi dell'arco che abbiamo appena scorso sappiamo perfettamente 
				 	chi e' la sorgente e chi no.
				 	Probabilmente la questione e' che potendoci essere l'arco sia in una direzione che nell'altra, io
				 	potrei gia' avere scorso l'arco nella direzione opposta di quella che sto considerando adesso e 
				 	quindi non voglio in realta' aggiungere il collegamento o comunque qualcosa del genere.
				 	
				 	Bisogna fare attenzione a questa parte e anche nell'esercizio Flights Delay e' stato affrontato.
				 	
				 	Il concetto e' che non bisogna salvarsi l'arco se l'arco attuale e' un arco inutile
					ai fini del percorso, e cioe' quando entrambi i nodi analizzati erano gia' stati 
					percorsi nella visita, perche' in quel caso stiamo creando un ciclo se aggiungiamo un
					arco di questo tipo.
					
					Alla fine ho commentato l'else if in quanto anche rivedendo la sua videolezione, secondo me il 
					professore era convinto di avere un grafo non orientato e allora in quel caso non sapendo bene la 
					direzione con cui stessimo scorrendo un arco bisognava fare quel controllo in piu' come fatto
					nell'esercizio fatto in aula di flightsDelay.
				 */
				if(albero.containsKey(a) && !albero.containsKey(b)) {
					// a è già noto, quindi ho scoperto b provenendo da a
					albero.put(b,a) ;
				} 
				/*else if(albero.containsKey(b) && !albero.containsKey(a)){
					// b è già noto, quindi ho scoperto a provenendo da b
					albero.put(a,b) ;
				}
				*/
			}
			
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {}
			
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {}
		});
		
		
		//ogni volta che next viene richiamato viene analizzato l'arco percorso
		while(bfv.hasNext()) {
			bfv.next() ; // estrai l'elemento e ignoralo
		}
		
		return albero ;
		
	}
	
	/*
	 	Funziona come la visita in ampiezza solo che qui andiamo piu' in profondita' possibile per poi 
	 	ritornare indietro ed esplorare un'altra strada.
	 	Rispetto alla visita in ampiezza cambia l'iteratore in quanto giustamente l'operazione che andiamo
	 	a fare e' un'altra.
	 */
	public List<Fermata> visitaProfondita(Fermata source) {
		List<Fermata> visita = new ArrayList<>();
		
		GraphIterator<Fermata, DefaultEdge> dfv = new DepthFirstIterator<>(graph, source);
		while(dfv.hasNext()) {
			visita.add( dfv.next() ) ;
		}
		
		return visita ;
	}
	
	/*
	 	Cerchiamo i cammini minimi tra due fermate usando Dijkstra che serve per avere tutti i cammini
	 	minimi per la singola fermata. 
	 	Con getPath() ci restituisce il percorso minimo tra due punti, mentre con getPaths() ci restituisce
	 	tutti i cammini minimi per tutti i possibili veritici di arrivo (partendo dalla sorgente).
	 */
	public List<Fermata> camminiMinimi(Fermata partenza, Fermata arrivo) {
		
		DijkstraShortestPath<Fermata, DefaultEdge> dij = new DijkstraShortestPath<>(graph);
		
		GraphPath<Fermata, DefaultEdge> cammino=dij.getPath(partenza, arrivo);
		
		
		return cammino.getVertexList() ;
	}
	
	public static void main(String args[]) {
		Model m = new Model() ;
		
		List<Fermata> visita1 = m.visitaAmpiezza(m.fermate.get(0));
		System.out.println("\nVISITA IN AMPIEZZA: \n");
		System.out.println(visita1);
		
		List<Fermata> visita2 = m.visitaProfondita(m.fermate.get(0));
		System.out.println("\nVISITA IN PROFONDITA': \n");
		System.out.println(visita2);

		Map<Fermata,Fermata> albero = m.alberoVisita(m.fermate.get(0)) ;
		System.out.println("\nALBERO VISITA: \n");
		for(Fermata f: albero.keySet()) {
			System.out.format( "%s <- %s\n", f, albero.get(f)) ;
		}
		
		List<Fermata> cammino=m.camminiMinimi(m.fermate.get(0), m.fermate.get(1));
		System.out.println("\nCAMMINO MINIMO: \n");
		System.out.println(cammino);
	}

}
