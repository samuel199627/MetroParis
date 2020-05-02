package it.polito.tdp.metroparis.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.javadocmd.simplelatlng.LatLng;

import it.polito.tdp.metroparis.model.CoppiaFermate;
import it.polito.tdp.metroparis.model.Fermata;
import it.polito.tdp.metroparis.model.Linea;

public class MetroDAO {

	//importiamo tutte le fermate dalla rispettiva tabella
	public List<Fermata> getAllFermate() {

		final String sql = "SELECT id_fermata, nome, coordx, coordy FROM fermata ORDER BY nome ASC";
		List<Fermata> fermate = new ArrayList<Fermata>();

		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Fermata f = new Fermata(rs.getInt("id_Fermata"), rs.getString("nome"),
						new LatLng(rs.getDouble("coordx"), rs.getDouble("coordy")));
				fermate.add(f);
			}

			st.close();
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Errore di connessione al Database.");
		}

		return fermate;
	}

	//importiamo tutte le linee dalla rispettiva tabella
	public List<Linea> getAllLinee() {
		final String sql = "SELECT id_linea, nome, velocita, intervallo FROM linea ORDER BY nome ASC";

		List<Linea> linee = new ArrayList<Linea>();

		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Linea f = new Linea(rs.getInt("id_linea"), rs.getString("nome"), rs.getDouble("velocita"),
						rs.getDouble("intervallo"));
				linee.add(f);
			}

			st.close();
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Errore di connessione al Database.");
		}

		return linee;
	}

	//ritorniamo vero se le fermate in parametro sono connesse, cioe' se si trova una linea in connessione
	//importante l'ordine perche' stiamo creando un grafo diretto
	//non ci importa quante linee ci siano che connettono, non vogliamo un multigrafo. Ci basta che ci sia una connessione
	public boolean fermateConnesse(Fermata fp, Fermata fa) {
		String sql = "SELECT COUNT(*) AS C " + "FROM connessione " + "WHERE id_stazP=? " + "AND id_stazA=?";

		try {
			Connection conn = DBConnect.getConnection();
			PreparedStatement st = conn.prepareStatement(sql);

			st.setInt(1, fp.getIdFermata());
			st.setInt(2, fa.getIdFermata());

			ResultSet res = st.executeQuery();

			res.first();
			int linee = res.getInt("C");

			conn.close();

			//se c'e' almeno una connessione dobbiamo creare l'arco nel grafo
			return linee >= 1;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	//data una stazione di partenza, ci facciamo restituire tutte le stazioni di arrivo.
	//Ci serve passare la mappa di tutte le stazioni (Identity Map) che avevamo perche' nella tabella
	//connessione abbiamo solo l'id della stazione e quindi ci serve la mappa per estrarre
	//l'oggetto fermata da aggiungere alla lista di oggetti Fermata che dobbiamo ritornare.
	//Nella Query mettiamo il distinct perche' nella mappa da cui c'e' il dataset ci sono
	//archi multiplic, ma noi vogliamo solo tenere una sola connessione orientata tra due
	//fermate e ce ne freghiamo degli archi multipli; per questo motivo mettiamo DISTINCT.
	public List<Fermata> fermateSuccessive(Fermata fp, Map<Integer, Fermata> fermateIdMap) {
		String sql = "SELECT DISTINCT id_stazA " + "FROM connessione " + "WHERE id_stazP=?";

		List<Fermata> result = new ArrayList<>();

		try {
			Connection conn = DBConnect.getConnection();

			PreparedStatement st = conn.prepareStatement(sql);

			st.setInt(1, fp.getIdFermata());

			ResultSet res = st.executeQuery();

			while (res.next()) {
				int id_fa = res.getInt("id_stazA"); // ID fermata arrivo
				result.add(fermateIdMap.get(id_fa));
			}

			conn.close();

			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	//interroghiamo la tabella Connessione che ci fornisce in ogni linea un arco del grafo
	//e quindi e' molto diretto interrogare cosi' il database per avere tutti gli archi del grafo che mi servono.
	//Anche qui abbiamo un bel DISTINCT perche' ci basta una sola connessione orientata tra due fermate
	//e non vogliamo implementare archi multipli
	public List<CoppiaFermate> coppieFermate(Map<Integer, Fermata> fermateIdMap) {
		String sql = "SELECT DISTINCT id_stazP, id_stazA FROM connessione" ;
		
		List<CoppiaFermate> result = new ArrayList<>();
		
		try {
			Connection conn = DBConnect.getConnection() ;
			PreparedStatement st = conn.prepareStatement(sql);
			ResultSet res = st.executeQuery();
			
			while(res.next()) {
				CoppiaFermate c = new CoppiaFermate(
						fermateIdMap.get(res.getInt("id_stazP")),
						fermateIdMap.get(res.getInt("id_stazA"))) ;
				result.add(c);
			}
			
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result ;
	}

}
