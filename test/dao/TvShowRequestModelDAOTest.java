package dao;

import models.TvShowRequest;
import models.User;
import models.dao.TvShowRequestDAO;
import models.dao.UserDAO;
import org.dbunit.JndiDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.*;
import play.db.Database;
import play.db.Databases;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import java.io.FileInputStream;
import java.util.List;

import static org.junit.Assert.*;

public class TvShowRequestModelDAOTest {
  private static Database db;
  private static JPAApi jpa;
  private JndiDatabaseTester databaseTester;
  private static TvShowRequestDAO tvShowRequestDAO;

  @BeforeClass
  static public void initDatabase() {
    // conectamos con la base de datos de test
    db = Databases.inMemoryWith("jndiName", "DefaultDS");
    db.getConnection();
    // activamos modo MySQL
    db.withConnection(connection -> {
      connection.createStatement().execute("SET MODE MySQL;");
    });
    jpa = JPA.createFor("memoryPersistenceUnit");
    tvShowRequestDAO = new TvShowRequestDAO(jpa);
  }

  @Before
  public void initData() throws Exception {
    databaseTester = new JndiDatabaseTester("DefaultDS");
    IDataSet initialDataSet = new FlatXmlDataSetBuilder().build(new
      FileInputStream("test/resources/tvShowRequest_dataset.xml"));
    databaseTester.setDataSet(initialDataSet);

    // Set up - CLEAN_INSERT: primero delete all y despues insert
    databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
    // On tear down - DELETE_ALL: primero borrar contenido del dataset
    databaseTester.setTearDownOperation(DatabaseOperation.DELETE_ALL);

    databaseTester.onSetup();
  }

  @After
  public void clearData() throws Exception {
    databaseTester.onTearDown();
  }

  // al final limpiamos la base de datos y la cerramos
  @AfterClass
  static public void shutdownDatabase() {
    jpa.shutdown();
    db.shutdown();
  }

  // testeamos crear una request
  @Test
  public void testTvShowRequestDAOCreate() {
    final UserDAO userDAO = new UserDAO(jpa);

    User user = jpa.withTransaction(() -> userDAO.find(1));

    TvShowRequest request1 = new TvShowRequest(222222, user);
    request1.status = TvShowRequest.Status.Requested;
    TvShowRequest request2 = jpa.withTransaction(() -> tvShowRequestDAO.create(request1));

    assertEquals(request1.tvdbId, request2.tvdbId);
    assertEquals(request1.user.id, request2.user.id);
    assertEquals(request1.requestDate, request2.requestDate);
    assertEquals(request1.status, request2.status);
  }

  // testeamos buscar una request
  @Test
  public void testTvShowRequestDAOFind() {
    TvShowRequest request = jpa.withTransaction(() -> tvShowRequestDAO.find(1));

    assertEquals(1, (int) request.id);
    assertEquals(111111, (int) request.tvdbId);
    assertEquals(1, (int) request.user.id);
  }

  // testeamos buscar por id -> not found
  @Test
  public void testTvShowRequestDAOFindNotFound() {
    TvShowRequest request = jpa.withTransaction(() -> tvShowRequestDAO.find(0));
    assertNull(request);
  }

  // testeamos obtener todos los requests
  @Test
  public void testTvShowRequestDAOAll() {
    List<TvShowRequest> requestsEncontrados = jpa.withTransaction(tvShowRequestDAO::all);
    assertEquals(1, requestsEncontrados.size());
  }

  // testeamos delete request OK
  @Test
  public void testTvShowRequestDAODelete() {
    TvShowRequest request = jpa.withTransaction(() -> {
      TvShowRequest s = tvShowRequestDAO.find(1);
      tvShowRequestDAO.delete(s);
      return tvShowRequestDAO.find(1);
    });

    assertNull(request);
  }

  // testeamos delete request not found
  @Test
  public void testTvShowRequestDAODeleteNotFound() {
    jpa.withTransaction(() -> {
      TvShowRequest request = tvShowRequestDAO.find(0);
      try {
        tvShowRequestDAO.delete(request);
      } catch (Exception e) {
        assertNull(request);
      }
    });
  }

  // testeamos delete cascade, borrando un user deberian borrarse sus requests
  @Test
  public void testTvShowRequestDAODeleteCascadeFromUser() {
    final UserDAO userDAO = new UserDAO(jpa);

    jpa.withTransaction(() -> {
      TvShowRequest request = tvShowRequestDAO.find(1);
      assertNotNull(request);

      userDAO.delete(userDAO.find(1));
      request = tvShowRequestDAO.find(1);
      assertNull(request);
    });
  }

}
