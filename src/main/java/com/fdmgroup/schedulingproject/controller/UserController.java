package com.fdmgroup.schedulingproject.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fdmgroup.schedulingproject.exception.PasswordDoesNotMatchException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyExistsException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.service.UserDetailsService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

	@Autowired
	private UserDetailsService userDetailsService;

	private Logger logger = LogManager.getLogger(UserController.class);

	@GetMapping("/")
	public String goToIndex(HttpSession session) {
		if (session.getAttribute("current_user") != null) {
			// user already logged in, skip login screen
			return "redirect:/home";
		}
		return "index";
	}

	@PostMapping("/login")
	public String verifyUser(@RequestParam String username, @RequestParam String password, HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			userDetailsService.verifyCredentials(username, password);
			session.setAttribute("current_user", username);
			logger.info("User with username " + username + " logged in");
			return "redirect:/home";
		} catch (UserNotFoundException | PasswordDoesNotMatchException e) {
			logger.error("User with username " + username + " attempted login but failed");
			redirectAttributes.addFlashAttribute("message", "Username or password is incorrect");
		}
		return "redirect:/";
	}

	@PostMapping("/create-user")
	public String createUser(@RequestParam String username, @RequestParam String password,
			RedirectAttributes redirectAttributes) {
		try {
			userDetailsService.createUser(username, password);
			logger.info("User with username" + username + " created");
			redirectAttributes.addFlashAttribute("message", "User " + username + " successfully created");
		} catch (UserAlreadyExistsException e) {
			logger.error("User with username " + username
					+ " attempted to be created but failed due to duplicated username");
			redirectAttributes.addFlashAttribute("message", "User " + username + " already exists");
		}
		return "redirect:/";
	}

	@GetMapping("/home")
	public String userHomePage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /home without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			User user = userDetailsService.getUserInfo(username);
			model.addAttribute("user", user.getDisplayName());
			model.addAttribute("pending", user.getReceivedContactInvites().size());
			model.addAttribute("eventInvites", user.getFutureEventInvites().size());
			logger.trace("User with username " + username + " loaded /home page");
		} catch (UserNotFoundException e) {
			// invalid session username
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		return "home";
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		String username = (String) session.getAttribute("current_user");
		if (username != null) {
			logger.info("User with username " + username + " logged out");
		}
		session.invalidate();
		return "redirect:/";
	}

	@GetMapping("/profile")
	public String userProfile(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /profile without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			// clone to prevent modifying user object in db
			User userCopy = (User) userDetailsService.getUserInfo(username).clone();
			// remove password for security
			userCopy.setPassword(null);
			model.addAttribute("user", userCopy);
			logger.trace("User with username " + username + " loaded /profile page");
			return "profile";
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
	}

	@PostMapping("/profile/update-display-name")
	public String updateDisplayName(@RequestParam String displayName, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /profile without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");

			return "redirect:/";
		}
		try {
			userDetailsService.updateDisplayName(username, displayName);
			logger.info("User with username " + username + " updated displayName");
			logger.debug("User with username " + username + " changed displayName to " + displayName);
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		return "redirect:/profile";
	}

	@PostMapping("profile/update-password")
	public String updatePassword(@RequestParam String currentPassword, @RequestParam String newPassword, @RequestParam String newPasswordConfirmation,
			HttpSession session, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /profile without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		if(!newPassword.equals(newPasswordConfirmation)) {
			redirectAttributes.addFlashAttribute("message","New password did not match");
			return "redirect:/profile";
		}
		try {
			userDetailsService.updatePassword(username, currentPassword, newPassword);
			logger.info("User with username " + username + " updated password");
			// password update success
			redirectAttributes.addFlashAttribute("message", "Password successfully updated");
			return "redirect:/profile";

		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		} catch (PasswordDoesNotMatchException e) {
			logger.error("User with username " + username
					+ " attempted to update password but failed due to providing incorrect current password");
			redirectAttributes.addFlashAttribute("message", "Current password did not match");
		}

		return "redirect:/profile";
	}

	@GetMapping("/calendar")
	public String userCalendar(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /calendar without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			User user = userDetailsService.getUserInfo(username);
			model.addAttribute("calendar", user.getFutureCalendar());
			model.addAttribute("calendarInvites", user.getFutureEventInvites());
			logger.trace("User with username " + username + " loaded /calendar page");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}

		return "calendar";
	}
}
