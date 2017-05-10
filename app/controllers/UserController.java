package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import models.service.UserService;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.Security.*;

import javax.inject.Inject;

public class UserController extends Controller {

  private final UserService userService;
  private final FormFactory formFactory;
  private final Auth sa;

  @Inject
  public UserController(UserService userService, FormFactory formFactory, Auth sa) {
    this.userService = userService;
    this.formFactory = formFactory;
    this.sa = sa;
  }

  @Transactional
  public Result register() {
    ObjectNode result = Json.newObject();

    String email;
    String password;
    String name;

    // obtenemos datos de la petición post
    DynamicForm requestForm = formFactory.form().bindFromRequest();

    try {
      email = String.valueOf(requestForm.get("email"));
      password = String.valueOf(requestForm.get("password"));
      name = String.valueOf(requestForm.get("name"));
    } catch (Exception ex) {
      result.put("error", "email/password/name null or not string");
      result.put("type", "bad request");
      result.put("message", "Campos incorrectos, contacte con un administrador");
      return badRequest(result);
    }

    // comprobamos si los datos están vacíos y si el email es válido y si ya está registrado
    if (email != null && !email.isEmpty() && password != null && !password.isEmpty() && name != null && !name.isEmpty()) {
      // comprobamos si el email es valido
      Constraints.EmailValidator emailValidator = new Constraints.EmailValidator();
      if (emailValidator.isValid(email)) {
        if (password.length() >= 6) {
          // comprobamos si el email ya está registrado
          if (userService.findByEmail(email) != null) {
            result.put("error", "email registered already");
            result.put("type", "bad request");
            result.put("message", "Este e-mail ya está registrado");
            return badRequest(result);
          }
        } else {
          result.put("error", "password not well generated");
          result.put("type", "bad request");
          result.put("message", "La contraseña no se ha generado bien");
          return badRequest(result);
        }
      } else {
        result.put("error", "email not valid");
        result.put("type", "bad request");
        result.put("message", "Este e-mail no es válido");
        return badRequest(result);
      }

      // intentamos crear usuario
      User user;

      try {
        user = userService.create(email, password, name);
      } catch (Password.CannotPerformOperationException | Password.InvalidHashException ex) {
        Logger.error(ex.getMessage());
        result.put("error", "exception creating hash");
        result.put("type", "internal server error");
        result.put("message", "No hemos podido registrar al usuario. Pruebe de nuevo más tarde");
        return internalServerError(result);
      } catch (javax.persistence.PersistenceException ex) {
        result.put("error", "email registered already");
        result.put("type", "bad request");
        result.put("message", "Este e-mail ya está registrado");
        return badRequest(result);
      }

      // si el usuario devuelto por la persistencia es nulo, ha ido mal
      if (user == null) {
        result.put("error","user null");
        result.put("type", "internal server error");
        result.put("message", "No hemos podido registrar al usuario. Pruebe de nuevo más tarde");
        return internalServerError(result);
      } else {
        // si se ha creado bien
        result.put("ok", "user created");
        result.put("type", "created");
        result.put("message", "Usuario registrado con éxito");
        return created(result);
      }
    } else {
      result.put("error", "empty field or fields");
      result.put("type", "bad request");
      result.put("message", "Ningún campo puede estar vacío");
      return badRequest(result);
    }
  }

}
