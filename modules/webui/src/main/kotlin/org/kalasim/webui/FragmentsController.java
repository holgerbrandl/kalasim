package org.kalasim.webui;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;

@Controller
public class FragmentsController {
    public FragmentsController() {
        super();
    }

    @GetMapping("/fragments")
    public String getHome() {
        return "fragments.html";
    }

    @GetMapping("/markup")
    public String markupPage() {
        return "markup.html";
    }

    @GetMapping("/params")
    public String paramsPage() {
        return "params.html";
    }

    @GetMapping("/other")
    public String otherPage(Model model) {
        model.addAttribute("data", new ArrayList <String>());
        return "other.html";
    }
}