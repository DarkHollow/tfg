package service;

import models.TvShow;
import models.dao.TvShowDAO;
import models.service.TvShowService;
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
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TvShowServiceTest {
  private static Database db;
  private static JPAApi jpa;
  private JndiDatabaseTester databaseTester;

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
  }

  @Before
  public void initData() throws Exception {
    databaseTester = new JndiDatabaseTester("DefaultDS");
    IDataSet initialDataSet = new FlatXmlDataSetBuilder().build(new
      FileInputStream("test/resources/tvShow_dataset.xml"));
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


  // testeamos crear una tv show
  @Test
  public void testTvShowServiceCreate() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    TvShow tvShow1 = new TvShow(3, "tvdbId", "Stranger Things", new Date(), "Descripción",
      "banner.jpg", "poster.jpg", "fanart.jpg", "Network", "45", null, "TV-14",
      TvShow.Status.Continuing, "Guionista", "Actor 1, Actor2", (float)9.0, "url_trailer");

    TvShow tvShow2 = jpa.withTransaction(() -> {
      return tvShowService.create(tvShow1);
    });

    assertEquals(tvShow1, tvShow2);
  }

  // testeamos buscar por id -> found
  @Test
  public void testTvShowServiceFind() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    TvShow tvShow = jpa.withTransaction(() -> {
      return tvShowService.find(1);
    });

    assertEquals(1, (int) tvShow.id);
    assertEquals(78804, (int) tvShow.tvdbId);
    assertEquals("Doctor Who (2005)", tvShow.name);
  }

  // testeamos buscar por id -> not found
  @Test
  public void testTvShowServiceFindNotFound() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    TvShow tvShow = jpa.withTransaction(() -> {
      return tvShowService.find(0);
    });

    assertNull(tvShow);
  }

  // testeamos buscar por tvdbId
  @Test
  public void testTvShowServiceFindByTvdbId() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    TvShow tvShow = jpa.withTransaction(() -> {
      return tvShowService.findByTvdbId(78804);
    });

    assertEquals(1, (int) tvShow.id);
    assertEquals(78804, (int) tvShow.tvdbId);
    assertEquals("Doctor Who (2005)", tvShow.name);
  }

  // testeamos buscar por tvdbId not found
  @Test
  public void testTvShowServiceFindByTvdbIdNotFound() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    TvShow tvShow = jpa.withTransaction(() -> {
      return tvShowService.findByTvdbId(0);
    });

    assertNull(tvShow);
  }

  // testeamos buscar por campo coincidendo exacto
  @Test
  public void testTvShowServiceFindByExact() {
    List<TvShow> tvShowsEncontrados = jpa.withTransaction(() -> {
      TvShowDAO tvShowDAO = new TvShowDAO(jpa);
      TvShowService tvShowService = new TvShowService(tvShowDAO);
      return tvShowService.findBy("name", "who", true);
    });

    assertEquals(0, tvShowsEncontrados.size());

    tvShowsEncontrados = jpa.withTransaction(() -> {
      TvShowDAO tvShowDAO = new TvShowDAO(jpa);
      TvShowService tvShowService = new TvShowService(tvShowDAO);
      return tvShowService.findBy("name", "Doctor Who (2005)", true);
    });

    assertEquals(1, tvShowsEncontrados.size());
    assertEquals("Doctor Who (2005)", tvShowsEncontrados.get(0).name);
  }

  // testeamos buscar por campo coincidiendo LIKE
  @Test
  public void testTvShowServiceFindByLike() {
    List<TvShow> tvShowsEncontrados = jpa.withTransaction(() -> {
      TvShowDAO tvShowDAO = new TvShowDAO(jpa);
      TvShowService tvShowService = new TvShowService(tvShowDAO);
      return tvShowService.findBy("name", "Who", false);
    });

    assertEquals(1, tvShowsEncontrados.size());
    assertEquals("Doctor Who (2005)", tvShowsEncontrados.get(0).name);
  }

  // testeamos obtener todas las tvShows
  @Test
  public void testTvShowServiceAll() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    List<TvShow> tvShowsEncontrados = jpa.withTransaction(() -> {
      return tvShowService.all();
    });

    assertEquals(2, tvShowsEncontrados.size());
  }

  // NOTE: update deprecated

  // testeamos delete tvshow
  @Test
  public void testTvShowServiceDelete() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    Boolean borrado = jpa.withTransaction(() -> {
      return tvShowService.delete(1);
    });

    assertTrue(borrado);
  }

  // testeamos delete tvshow not found
  @Test
  public void testTvShowServiceDeleteNotFound() {
    TvShowDAO tvShowDAO = new TvShowDAO(jpa);
    TvShowService tvShowService = new TvShowService(tvShowDAO);
    Boolean borrado = jpa.withTransaction(() -> {
      return tvShowService.delete(0);
    });

    assertFalse(borrado);
  }

}
