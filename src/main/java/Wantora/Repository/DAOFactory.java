package Wantora.Repository;

import java.sql.SQLException;
public class DAOFactory {
	private static IDAO dao = null;
        public static IDAO getDAOInstance(TypeDAO type) throws SQLException{
           	if(type == TypeDAO.MySQL){
				dao = new MySQLDAO();
				} 
		return dao;
	}       
}