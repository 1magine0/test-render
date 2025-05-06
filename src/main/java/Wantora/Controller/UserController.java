package Wantora.Controller;

import Wantora.Models.User;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import Wantora.Repository.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Objects;

@Controller
public class UserController {
    public IDAO dao;
    public SecretKey key;
    public UserController() {
        try {
            dao = DAOFactory.getDAOInstance(TypeDAO.MySQL);
            key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public boolean isValidPassword(String password) {
        return password.matches("^(?=.*\\d).{8,}$");
    }

    public boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailRegex);
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 1 day
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/wantoraMain")
    public String welcome(HttpServletRequest request, HttpServletResponse response, Model model) {
        User currentUser = (User) request.getSession().getAttribute("user");

        if (currentUser == null) {
            if (request.getCookies() != null) {
                String token = null;
                for (Cookie cookie : request.getCookies()) {
                    if ("authToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }

                if (token != null && validateToken(token)) {
                    String email = Jwts.parser()
                            .setSigningKey(key)
                            .parseClaimsJws(token)
                            .getBody()
                            .getSubject();
                    currentUser = dao.getUserByEmail(email);
                    if (currentUser != null) {
                        request.getSession().setAttribute("user", currentUser);
                    }
                }
            }
        }

        boolean isAuthenticated = (currentUser != null);
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("currentUser", currentUser);
        return "welcomePage";
    }


    @GetMapping("/login")
    public String loginGet(Model model) {
        User user = new User();
        model.addAttribute("user", user);
        return "login";
    }

    @PostMapping("/login")
    public String loginPost(@ModelAttribute("user") User user,
                            Model model, HttpServletRequest request, HttpServletResponse response, BindingResult bindingResult) {
        User dbUser = dao.getUserByEmail(user.getEmail());
        if (dbUser == null) {
            model.addAttribute("loginError", true);
            return "login";
        } else if (dbUser.getPassword().equals(dao.hashPassword(user.getPassword()))) {
            if (dbUser.getImageName() == null) {
                dbUser.setImageName("null");
            }
            String token = generateToken(dbUser);
            Cookie cookie = new Cookie("authToken", token);
            cookie.setMaxAge(60 * 60 * 24 * 30);
            cookie.setPath("/");
            response.addCookie(cookie);
            request.getSession().setAttribute("user", dbUser);

            return "redirect:/wantoraMain";
        } else {
            model.addAttribute("loginError", true);
            return "login";
        }
    }


    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerPost(@ModelAttribute("user") User user,
                               Model model, HttpServletRequest request, HttpServletResponse response, BindingResult bindingResult) {
        if (!dao.createUser(user)) {
            model.addAttribute("registrationError", true);
            return "register";
        } else {
            user = dao.getUserByEmail(user.getEmail());
            String token = generateToken(user);
            Cookie cookie = new Cookie("authToken", token);
            cookie.setMaxAge(60 * 60 * 24 * 30);
            cookie.setPath("/");
            response.addCookie(cookie);
            request.getSession().setAttribute("user", user);

            return "redirect:/wantoraMain";
        }
    }


    @GetMapping("/profile")
    public String getProfile(HttpServletRequest request, Model model) {
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", currentUser);
        model.addAttribute("newPassword", "");
        return "profile";
    }


    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") User updatedUser,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam(value = "action") String action,
                                HttpServletRequest request, HttpServletResponse response, Model model) {
        if ("delete".equals(action)) {
            System.out.println(updatedUser.toString());
            dao.deleteUser(updatedUser);
            request.getSession().invalidate();
            Cookie cookie = new Cookie("authToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
            return "redirect:/wantoraMain";
        } else {
            User currentUser = (User) request.getSession().getAttribute("user");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate birthDate = LocalDate.parse(updatedUser.getDate(), formatter);
            System.out.println(updatedUser.toString());
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            if (currentUser == null) {
                return "redirect:/login";
            }

            if (age < 12) {
                model.addAttribute("ageError", true);
                return "profile";
            }

            if (updatedUser.getPassword().isEmpty()) {
                updatedUser.setPassword(currentUser.getPassword());
                if (currentUser.equals(updatedUser)) {
                    model.addAttribute("updateError", true);
                    return "profile";
                }
                if (!isValidEmail(updatedUser.getEmail())) {
                    model.addAttribute("newEmailError", true);
                    return "profile";
                }
                dao.updateUser(updatedUser);
                request.getSession().setAttribute("user", updatedUser);
                String newToken = generateToken(updatedUser);
                Cookie cookie = new Cookie("authToken", newToken);
                cookie.setMaxAge(60 * 60 * 24 * 30);
                cookie.setPath("/");
                response.addCookie(cookie);
                model.addAttribute("updateSuccess", true);
                return "profile";
            } else if (!dao.hashPassword(updatedUser.getPassword()).equals(currentUser.getPassword())) {
                model.addAttribute("updatePasswordError", true);
                return "profile";
            } else if (currentUser.getPassword().equals(newPassword)) {
                model.addAttribute("updatePasswordEqualError", true);
                return "profile";
            } else if (newPassword.isEmpty()) {
                model.addAttribute("updatePasswordEmptyError", true);
                return "profile";
            } else if (!isValidPassword(newPassword)) {
                model.addAttribute("newPasswordError", true);
                return "profile";
            } else {
                if (!isValidEmail(updatedUser.getEmail())) {
                    model.addAttribute("newEmailError", true);
                    return "profile";
                }
                updatedUser.setPassword(dao.hashPassword(newPassword));
                dao.updateUser(updatedUser);
                request.getSession().setAttribute("user", updatedUser);
                String newToken = generateToken(updatedUser);
                Cookie cookie = new Cookie("authToken", newToken);
                cookie.setMaxAge(60 * 60 * 24 * 30);
                cookie.setPath("/");
                response.addCookie(cookie);
                model.addAttribute("updateSuccess", true);
                return "profile";
            }
        }
    }

    @PostMapping("/updatePfp")
    public String updatePfp(@RequestParam(value = "avatar") MultipartFile avatar,
                            HttpServletRequest request, HttpServletResponse response, Model model) {
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String fileName = avatar.getOriginalFilename();
                String extension = "";
                assert fileName != null;
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = fileName.substring(dotIndex);
                }

                String hashedFileName = dao.hashPassword(fileName);
                fileName = hashedFileName + extension;
                String realPath = request.getServletContext().getRealPath("/");
                String uploadDir = realPath + "uploads\\images\\users";
                File uploadDirFile = new File(uploadDir);
                if (!uploadDirFile.exists()) {
                    uploadDirFile.mkdirs();
                }

                File destinationFile = new File(uploadDirFile, fileName);
                avatar.transferTo(destinationFile);

                currentUser.setImageName(fileName);
                dao.updatePfp(currentUser);
                request.getSession().setAttribute("user", currentUser);

                String newToken = generateToken(currentUser);
                Cookie cookie = new Cookie("authToken", newToken);
                cookie.setMaxAge(60 * 60 * 24 * 30);
                cookie.setPath("/");
                response.addCookie(cookie);

                model.addAttribute("updateSuccess", true);
                return "redirect:/profile";
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("user", currentUser);
                model.addAttribute("fileUploadError", true);
                return "profile";
            }
        } else {
            model.addAttribute("user", currentUser);
            model.addAttribute("fileUploadError", true);
            return "profile";
        }
    }

    @PostMapping("/userLogout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        Cookie cookie = new Cookie("authToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/wantoraMain";
    }

}
