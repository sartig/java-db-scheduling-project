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

import com.fdmgroup.schedulingproject.exception.CannotInviteSelfException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInContactsException;
import com.fdmgroup.schedulingproject.exception.UserAlreadyInvitedException;
import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.exception.UserNotInvitedException;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.service.UserContactService;
import com.fdmgroup.schedulingproject.service.UserDetailsService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ContactController {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private UserContactService userContactService;

	private Logger logger = LogManager.getLogger(ContactController.class);

	@GetMapping("contacts")
	public String goToContacts(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String username = (String) session.getAttribute("current_user");
		if (username == null) {
			// if user navigates manually to /contacts without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			User user = userDetailsService.getUserInfo(username);
			model.addAttribute("contacts", user.getContacts());
			model.addAttribute("receivedInvites", user.getReceivedContactInvites());
			model.addAttribute("sentInvites", user.getSentContactInvites());
			logger.trace("User with username " + username + " loaded /contacts page");
		} catch (UserNotFoundException e) {
			return "redirect:/";
		}
		return "contacts";
	}

	@PostMapping("contacts/invite")
	public String sendContactInvite(@RequestParam String username, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String myUsername = (String) session.getAttribute("current_user");
		if (myUsername == null) {
			// if user navigates manually to /contacts without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			userContactService.sendContactInvite(myUsername, username);
			redirectAttributes.addFlashAttribute("message", "Sent invite to " + username);
			logger.info("User with username " + myUsername + " sent contact invite to user with username " + username);
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "User could not be found");
		} catch (UserAlreadyInContactsException e) {
			redirectAttributes.addFlashAttribute("message", "User " + username + " already in contacts");
		} catch (UserAlreadyInvitedException e) {
			redirectAttributes.addFlashAttribute("message", "User " + username + " already invited");
		} catch (CannotInviteSelfException e) {
			redirectAttributes.addFlashAttribute("message", "Cannot send contact invite to self");
		}
		return "redirect:/contacts";
	}

	@GetMapping("contacts/remove")
	public String removeContact(@RequestParam String username, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String myUsername = (String) session.getAttribute("current_user");
		if (myUsername == null) {
			// if user navigates manually to /contacts without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}
		try {
			userContactService.removeFromContacts(myUsername, username);
			redirectAttributes.addFlashAttribute("message", "User " + username + " removed from contacts");
			logger.info("User with username " + myUsername + " removed user with username " + username + " from contacts");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "User could not be found");
		}
		return "redirect:/contacts";
	}

	@GetMapping("contacts/accept")
	public String acceptContact(@RequestParam String username, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String myUsername = (String) session.getAttribute("current_user");
		if (myUsername == null) {
			// if user navigates manually to /contacts without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}

		try {
			userContactService.acceptContact(myUsername, username);
			redirectAttributes.addFlashAttribute("message", "User " + username + " added to contacts");
			logger.info("User with username " + myUsername + " accepted contact request from user with username " + username);
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "User could not be found");
		} catch (UserNotInvitedException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find invite from user " + username);
		} catch (CannotInviteSelfException e) {
			redirectAttributes.addFlashAttribute("message", "Cannot accept invitation from self");

		}
		return "redirect:/contacts";
	}

	@GetMapping("contacts/cancel")
	public String cancelContactInvite(@RequestParam String username, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String myUsername = (String) session.getAttribute("current_user");
		if (myUsername == null) {
			// if user navigates manually to /contacts without logging in
			redirectAttributes.addFlashAttribute("message", "Please log in");
			return "redirect:/";
		}

		try {
			userContactService.cancelContactInvite(myUsername, username);
			redirectAttributes.addFlashAttribute("message", "Invitation to user " + username + " removed");
			logger.info("User with username " + myUsername + " cancelled contact request to user with username " + username);
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", "User could not be found");
		} catch (UserNotInvitedException e) {
			redirectAttributes.addFlashAttribute("message", "Could not find invite to user " + username);
		} catch (CannotInviteSelfException e) {
			redirectAttributes.addFlashAttribute("message", "Cannot cancel invitation from self");
		}
		return "redirect:/contacts";
	}
}
