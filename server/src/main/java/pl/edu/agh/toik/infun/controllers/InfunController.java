package pl.edu.agh.toik.infun.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.toik.infun.exceptions.*;
import pl.edu.agh.toik.infun.model.Room;
import pl.edu.agh.toik.infun.model.domain.UserResult;
import pl.edu.agh.toik.infun.model.domain.TaskResult;
import pl.edu.agh.toik.infun.model.requests.*;
import pl.edu.agh.toik.infun.services.IFolderScanService;
import pl.edu.agh.toik.infun.services.IRoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Controller
//todo hide all functions body to service
public class InfunController {

    @Autowired
    IRoomService roomService;

    @Autowired
    IFolderScanService folderScanService;

    @RequestMapping("/")
    String main() {
        return "redirect:/room/join";
    }

    @RequestMapping("/room/create")
    String createRoom(Model model, @CookieValue("JSESSIONID") String cookie) {
        CreateRoomInput createRoomInput = new CreateRoomInput(roomService.createDefaultTasksConfig(
                folderScanService.scanFolder())
        );
        model.addAttribute("createRoomInput", createRoomInput);
        return "create_room";
    }

    @GetMapping(value = "/room/join")
    String joinRoom(Model model, @CookieValue("JSESSIONID") String cookie) {
        model.addAttribute("joinRoomInput", new JoinRoomInput());
        return "join_room";
    }

    @PostMapping(value = "/room/join")
    String getTask(@ModelAttribute JoinRoomInput joinRoomInput, @CookieValue("JSESSIONID") String cookie, Model model) throws NoSuchUserException, UserAlreadyExistsException, NoSuchRoomException {
        roomService.addUser(joinRoomInput.nick, joinRoomInput.age, joinRoomInput.roomId, cookie);
        return "redirect:/tasks/new";
    }

    @GetMapping("/tasks/new")
    public String getNextTask(@CookieValue("JSESSIONID") String cookie) throws NoSuchUserException {
        try {
            return roomService.getNextTask(cookie) + "/index";
        } catch (NoMoreAvailableTasksException e) {
            return "redirect:/end";
        }
    }

    @RequestMapping("/manage")
    String manage(@ModelAttribute("createRoomInput") CreateRoomInput createRoomInput, @CookieValue("JSESSIONID") String cookie, Model model) throws RoomAlreadyExistsException, NoGameSelectedException {
        List<TaskConfig> userChoice = createRoomInput.getTasksConfig();
        String roomId = createRoomInput.getRoomId();
        if (roomId == null || roomId.trim().equals("")) {
            roomId = roomService.generateRandomRoomId();
        }
        List<TaskConfig> filteredConfigs = userChoice.stream().filter(taskConfig -> taskConfig.name != null).collect(Collectors.toList());

        if (filteredConfigs.size() == 0) {
            throw new NoGameSelectedException("Nie wybrano żadnej gry");
        }
        List<TaskConfig> configs = new ArrayList<>();
        for (TaskConfig taskConfig : filteredConfigs) {
            configs.add(new TaskConfig(taskConfig.name.trim(), taskConfig.config == null ? new ArrayList<>() : taskConfig.config));
        }

        roomService.addRoom(new Room(roomId, configs, cookie, createRoomInput.getTaskNumber()));
        model.addAttribute("room_id", roomId);
        return "manage";
    }

    @RequestMapping("/{task_name}/config")
    @ResponseBody
    String getConfig(@PathVariable(value = "task_name") final String taskName, @CookieValue("JSESSIONID") String cookie) throws NoSuchUserException {
        return roomService.getConfig(taskName, cookie);
    }


    @PostMapping(value = "/{task_name}/end")
    @ResponseBody
    public String endGame(@PathVariable(value = "task_name") final String taskName, @RequestBody TaskResult taskResult, @CookieValue("JSESSIONID") String cookie, Model model) throws NoSuchRoomException, NoSuchUserException {
        roomService.addResult(taskName, cookie, taskResult.getNick(), taskResult.getRoom(), taskResult.getResult());
        model.addAttribute("result", taskResult);
        return "/task_result";
    }

    @GetMapping(value = "/task_result")
    String taskResult(@CookieValue("JSESSIONID") String cookie, Model model) {
        return "task_result";
    }

    @GetMapping(value = "/end")
    String end(@CookieValue("JSESSIONID") String cookie, Model model) {
        model.addAttribute("result", "Udało Ci się skończyć wszystkie zadania. \nCałkowity rezultat = " + roomService.getLastResults(cookie).getScore());
        return "end";
    }

    @GetMapping(value = "/{room_id}/remove")
    String endGame(@CookieValue("JSESSIONID") String cookie, @PathVariable(value = "room_id") final String roomId) throws CannotRemoveRoomException {
        roomService.removeRoom(roomId, cookie);
        return "redirect:/";
    }

    @RequestMapping("/{room_id}/results")
    @ResponseBody
    List<UserResult> getResults(@PathVariable(value = "room_id") final String roomId, @CookieValue("JSESSIONID") String cookie) throws NoSuchUserException, NoSuchRoomException, AccessDeniedException {
        System.out.println("RESULTS: " + roomService.getResults(roomId, cookie));
        return roomService.getResults(roomId, cookie);
    }

    @RequestMapping("/last/results")
    @ResponseBody
    LastResultResponse getLastResults(@CookieValue("JSESSIONID") String cookie) throws NoSuchUserException {
        return roomService.getLastResults(cookie);
    }
}