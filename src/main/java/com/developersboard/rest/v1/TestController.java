package com.developersboard.rest.v1;

import com.developersboard.annotation.Loggable;
import com.developersboard.domain.Test.Test;
import com.developersboard.repository.impl.TestRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test")
public class TestController {

  @Autowired private TestRepository testRepository;

  @PostMapping
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Test> save(@RequestBody Test test) {
    Test saved = testRepository.save(test);

    return ResponseEntity.ok(saved);
  }

  @PutMapping("/update")
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Test> update(@RequestBody Test test) {
    Test saved = testRepository.findById(test.getId()).get();
    saved.setEmail(test.getEmail());

    var update = testRepository.save(saved);
    return ResponseEntity.ok(update);
  }

  @GetMapping("")
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<List<Test>> getAll() {
    return ResponseEntity.ok(testRepository.findAll());
  }
}
