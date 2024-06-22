package com.developersboard.rest.v1;

import com.developersboard.annotation.Loggable;
import com.developersboard.domain.Test.Test;
import com.developersboard.repository.impl.TestRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tests")
public class TestController {

  @Autowired private final TestRepository testRepository;

  @PostMapping
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Test> save(@RequestBody Test test) {
    Test saved = testRepository.save(test);
    return ResponseEntity.ok(saved);
  }

  @PutMapping("/{id}")
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Test> update(@PathVariable Long id, @RequestBody Test test) {
    Optional<Test> existingTestOpt = testRepository.findById(id);
    if (existingTestOpt.isPresent()) {
      Test existingTest = existingTestOpt.get();
      existingTest.setEmail(test.getEmail());
      // Update other fields as needed
      Test updated = testRepository.save(existingTest);
      return ResponseEntity.ok(updated);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("")
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Page<Test>> getAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String[] sort,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email) {

    // Creating Pageable object
    Pageable pageable = PageRequest.of(page, size, Sort.by(getSortOrders(sort)));

    // Building the Specification for filtering
    Specification<Test> spec = Specification.where(null);

    if (username != null) {
      spec = spec.and(TestSpecifications.hasUsername(username));
    }

    if (email != null) {
      spec = spec.and(TestSpecifications.hasEmail(email));
    }

    // Fetching the data
    Page<Test> tests = testRepository.findAll(pageable);

    return ResponseEntity.ok(tests);
  }

  @GetMapping("/{id}")
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Test> getById(@PathVariable Long id) {
    Optional<Test> test = testRepository.findById(id);
    return test.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  @SecurityRequirements
  @Loggable(level = "debug")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (testRepository.existsById(id)) {
      testRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  // Helper method to convert sort parameters to Sort.Order
  private List<Sort.Order> getSortOrders(String[] sort) {
    List<Sort.Order> orders = new ArrayList<>();
    for (String sortOrder : sort) {
      String[] _sort = sortOrder.split(",");
      orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
    }
    return orders;
  }
}

class TestSpecifications {

  public static Specification<Test> hasUsername(String username) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("username"), username);
  }

  public static Specification<Test> hasEmail(String email) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("email"), email);
  }
}
