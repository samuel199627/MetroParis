package it.polito.tdp.metroparis.model;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import it.polito.tdp.metroparis.db.MetroDAO;

//IL MODEL E' QUI

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
	
	public static void main(String args[]) {
		new Model() ;
	}

}
