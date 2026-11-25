// The upstream Eureka server JAR ships an empty wro.js, so the Bootstrap
// navbar/collapse behaviour is missing. Load Bootstrap from the local WebJar
// instead of a CDN so the dashboard keeps working offline.
(function () {
  var script = document.createElement("script");
  script.src = "/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js";
  document.head.appendChild(script);
})();
